package upem.jarret.server;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.fasterxml.jackson.databind.ObjectMapper;

import upem.jarret.json.Job;
import upem.jarret.json.JsonManipulation;

public class Server {
	private static class Context {
		private boolean inputClosed = false;
		private final ByteBuffer in = ByteBuffer.allocate(BUF_SIZE);
		private final ByteBuffer out = ByteBuffer.allocate(BUF_SIZE);
		private final SelectionKey key;
		private final SocketChannel sc;

		public Context(SelectionKey key) {
			this.key = key;
			this.sc = (SocketChannel) key.channel();
		}

		public void doRead() throws IOException {
			if (sc.read(in) == -1) {
				inputClosed = true;
			}
			process();
			updateInterestOps();
		}

		public void doWrite() throws IOException {
			out.flip();
			sc.write(out);
			out.compact();
			process();
			updateInterestOps();
		}

		private void process() {
			in.flip();

			while (in.remaining() >= 2 * Integer.BYTES && out.remaining() >= Integer.BYTES) {
				out.putInt(in.getInt() + in.getInt());
			}
			in.compact();
		}

		private void updateInterestOps() {

			int ops = 0;

			if (out.position() != 0) {
				ops = ops | SelectionKey.OP_WRITE;
			}

			if (in.hasRemaining() && !inputClosed) {
				ops = ops | SelectionKey.OP_READ;
			}
			if (ops == 0) {

				silentlyClose(sc);
				return;
			}
			key.interestOps(ops);
		}

	}

	private static final int BUF_SIZE = 4096;
	private static final long TIMEOUT = 5_000;
	private final ServerConfig serverConfig;
	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private final Set<SelectionKey> selectedKeys;
	private final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(4);
	private boolean stopAccept = false;
	private final ObjectMapper mapper = new ObjectMapper();

	private final Map<Job, JobInfos> jobs;
	private final ArrayList<Job> jobsTasks  = new ArrayList<>();

	private final int arraySize;
	private int currentIndex = 0;

	private int currentIndexArrayJobsTasks  = 0;
	private boolean noTaskNow = false;

	public Server(int port,String file) throws Exception {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		selector = Selector.open();
		selectedKeys = selector.selectedKeys();
		serverConfig = JsonManipulation.parseServerConfig(file);
		jobs = JobManager.init(serverConfig.getAnswersDirectory(),serverConfig.getLogDirectory());
		System.out.println(serverConfig);
		initJobsTasks();
		arraySize = jobs.keySet().size();
		
		jobs.values().forEach(jobInfo -> System.out.println("AnswersDirectory : "+jobInfo.getFileContainsResponses()+"\n"
				+ "LogsDirectory : "+jobInfo.getFileContainsLog()));
		
	}

	private void initJobsTasks(){
		jobs.keySet().forEach(job -> jobsTasks.add(job));
	}

	private boolean verifyAvailableTask(){
		for(JobInfos info : jobs.values()){
			if(!info.verifyDoneAllTasks()){
				return true;
			}
		}
		return false;
	}

	public void launch() throws IOException {
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		Set<SelectionKey> selectedKeys = selector.selectedKeys();
		while (!Thread.interrupted()) {
		    if(verifyAvailableTask()){
		    	System.out.println(getTask().toString());
		    }
		    try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//selector.select();          
            //processSelectedKeys();
			//selectedKeys.clear();
		}
	}
	public Optional<String> getTask(){

		Optional<String> result = Optional.empty();
		currentIndexArrayJobsTasks %= arraySize; 

		Job job = jobsTasks.get(currentIndexArrayJobsTasks);
		if(jobs.get(job).verifyDoneAllTasks()){
			currentIndexArrayJobsTasks++;
			currentIndex = 0; 
			return getTask();
		}
		if(jobs.get(job).getTasks().hasNext()){
			result = jobs.get(job).getTasks().nextTask(); //On renvoie la prochaine tâche pas encore acquitée
			currentIndex++;

			if(!jobs.get(job).getTasks().hasNext()){
				jobs.get(job).setStatusOfDoneAllTasks();

			}
			
		}else{
			jobs.get(job).setStatusOfDoneAllTasks();	  //Si pas de suivant alors toutes les tâches ont été traitées
		}
		if(currentIndex == job.getJobPriority() ){
			//On passe aux tâches des jobs suivants
			currentIndexArrayJobsTasks++;
			currentIndex = 0; 
		}
		return result;
	}

	private ByteBuffer createTaskAnswer(String task) {
		StringBuilder body = new StringBuilder();
		ByteBuffer bb = UTF8_CHARSET.encode(task);
		body.append("HTTP/1.1 200 OK\r\nContent-type: application/json; charset=utf-8\r\nContent-length: ")
		.append(bb.remaining()).append("\r\n\r\n");
		ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);
		buffer.put(UTF8_CHARSET.encode(body.toString()));
		buffer.put(bb);
		return buffer;
	}

	private ByteBuffer createWaitAnswer() {
		StringBuilder header = new StringBuilder();
		StringBuilder body = new StringBuilder();
		body.append("{ \"ComeBackInSeconds\" : 300 }");

		int length = UTF8_CHARSET.encode(body.toString()).remaining();

		header.append("HTTP/1.1 200 OK\r\nContent-type: application/json; charset=utf-8\r\nContent-length: ")
		.append(length).append("\r\n\r\n").append(body.toString());
		ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);
		buffer.put(UTF8_CHARSET.encode(header.toString()));
		buffer.put(UTF8_CHARSET.encode(body.toString()));
		return buffer;
	}

	private String generateErrorAnswer() {
		StringBuilder httpError = new StringBuilder();
		httpError.append("HTTP/1.1 400").append("\r\n\r\n");

		return httpError.toString();
	}

	private void processSelectedKeys() throws IOException {
		for (SelectionKey key : selectedKeys) {
			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
			}
			try {
				Context cntxt = (Context) key.attachment();
				if (key.isValid() && key.isWritable()) {
					cntxt.doWrite();
				}
				if (key.isValid() && key.isReadable()) {
					cntxt.doRead();
				}
			} catch (IOException e) {
				silentlyClose(key.channel());
			}
		}

	}

	private void doAccept(SelectionKey key) throws IOException {
		if (stopAccept) {
			return;
		}
		SocketChannel sc = serverSocketChannel.accept();
		sc.configureBlocking(false);
		SelectionKey clientKey = sc.register(selector, SelectionKey.OP_READ);
		clientKey.attach(new Context(clientKey));
	}

	private static void silentlyClose(SelectableChannel sc) {
		if (sc == null)
			return;
		try {
			sc.close();
		} catch (IOException e) {
			// silently ignore
		}
	}

	public void processCommand() {
		String command;
		while ((command = queue.poll()) != null) {
			switch (command) {
			case "SHUTDOWN NOW":
				System.out.println("server is shutdown");
				shutdownNow();
				break;
			case "SHUTDOWN":
				System.out.println("server doesn't take new clients");
				shutdown();
				break;
			case "INFO":
				System.out.println("killing clients");
				show();
				break;
			}
		}
	}

	private void shutdownNow() {
		silentlyClose(serverSocketChannel);
		shutdown();
		Thread.currentThread().interrupt();
	}

	private void shutdown() {
		silentlyClose(serverSocketChannel);
	}

	private void show() {
		System.out.println("there is " + String.valueOf(selector.keys().size() - 1) + " clients connected");

	}

	private static void usage() {
		System.out.println("Server <listeningPort>");
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			usage();
			return;
		}
		Server server = new Server(7777,"./src/JarRetConfig.json");
		server.launch();

	}

}
