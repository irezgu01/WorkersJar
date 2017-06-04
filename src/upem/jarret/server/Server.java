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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import upem.jarret.client.HTTPHeader;
import upem.jarret.json.Job;
import upem.jarret.json.JsonManipulation;

public class Server {

	public static final int BUF_SIZE = 4096;
	public static final long TIMEOUT = 5_000;
	public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	private final ServerConfig serverConfig;
	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private final Set<SelectionKey> selectedKeys;
	private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(4);
	private boolean stopAccept = false;
	private final ObjectMapper mapper = new ObjectMapper();

	private final Map<Job, JobInfos> jobs;
	private final Map<Integer, Job> mapAssociatedJobId;
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
		mapAssociatedJobId = new HashMap<>();
		jobs.keySet().forEach(job -> {
			int id = job.getJobId();
			mapAssociatedJobId.put(id, job);
		});
		//System.out.println(serverConfig);
		initJobsTasks();
		arraySize = jobs.keySet().size();
		/*
		jobs.values().forEach(jobInfo -> System.out.println("AnswersDirectory : "+jobInfo.getFileContainsResponses()+"\n"
				+ "LogsDirectory : "+jobInfo.getFileContainsLog()));
		 */
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

			System.out.println("Starting select");
			selector.select();          
			processSelectedKeys();
			selectedKeys.clear();
			if(!verifyAvailableTask()){
				System.out.println("End Service!");
				return;
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Finishing select");
			selectedKeys.clear();

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

	public ByteBuffer createTaskAnswer(String task) throws IOException {
		StringBuilder body = new StringBuilder();
		ByteBuffer jsonBuffer = UTF8_CHARSET.encode(task);

		body.append("HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: ")
		.append(jsonBuffer.remaining()).append("\r\n\r\n");
		ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);
		buffer.put(UTF8_CHARSET.encode(body.toString()));
		buffer.put(jsonBuffer);
		return buffer;
	}

	public ByteBuffer createWaitAnswer() {
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
	private String generateOKAnswer() {
		StringBuilder sb = new StringBuilder();
		sb	.append("HTTP/1.1 200 OK\r\nContent-length: ")
		.append("0")
		.append("\r\n\r\n");
		
		return sb.toString();
	}

	private void processSelectedKeys() throws IOException {
		for (SelectionKey key : selectedKeys) {
			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
			}

			if (key.isValid() && key.isWritable()) {
				System.out.println("Do write process ...");
				try {
					doWrite(key);
				} catch (IOException e) {
					//Nothing
				}
			}
			if (key.isValid() && key.isReadable()) {
				System.out.println("Do read process ...");
				try {
					doRead(key);
				} catch (IOException e) {
					//Nothing
				}
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
	public void doRead(SelectionKey key) throws IOException {
		System.out.println("Debut doRead");
		Context cntxt = (Context) key.attachment();
		if (cntxt.read() == -1) {
			cntxt.setInputClosed(true);
		}
		process(cntxt);
		cntxt.getIn().flip();
		updateInterestOps(key);
		System.out.println("Fin doRead");
	}

	public void doWrite(SelectionKey key) throws IOException {
		System.out.println("Debut doWrite");
		Context cntxt = (Context) key.attachment();
		cntxt.write();
		process(cntxt);
		System.out.println("Fin doWrite");
		updateInterestOps(key);
	}
	private void sendTaskToClient(ByteBuffer out) throws IOException{
		String task = getTask().orElse("");
		if(task.equals("")){
			System.out.println("Oups");
			return;
		}
		System.out.println("Client send a GET request ...");
		System.out.println("Task sending by the server to client : \n"+task+"\n");
		ByteBuffer bb = createTaskAnswer(task);
		bb.flip();
		out.put(bb);
	}

	private void process(Context cntxt) throws IOException {

		System.out.println("Debut process");
		ByteBuffer in = cntxt.getIn();
		ByteBuffer out = cntxt.getOut();

		if(!cntxt.GetIsOK()){
			
			System.out.println("PARTIE GET");
			String getRequest = cntxt.getReader().readLineCRLF();
			if(getRequest.indexOf("GET Task")!=-1){
				sendTaskToClient(out);
			}
			cntxt.setGetIsOK(true);
		}
		else if(cntxt.GetIsOK()){
			Optional<HashMap<String, String>>  header = cntxt.getReader().readHeader();
			ObjectNode json = null;
			if(header.isPresent()){
				HashMap<String, String> validHeader = header.get();
	
				if(validHeader.get("response").indexOf("POST Answer") != -1){
					Optional<ByteBuffer> content = cntxt.getReader().readBytes(Integer.parseInt(validHeader.get("Content-Length").substring(1)));
					if(content.isPresent()){
						ByteBuffer bb = content.get();
						bb.flip();
						int t = bb.remaining();
						String response = UTF8_CHARSET.decode(bb).toString();
						System.out.println("La reponse du client "+response+"\n"+t);
						
						try {
							json = JsonManipulation.tojson(response);
						} catch (JsonProcessingException e) {
							//On répond un code 400 au client
							ByteBuffer feedback = UTF8_CHARSET.encode(generateErrorAnswer());
							out.put(feedback);
							cntxt.setGetIsOK(false);
							System.out.println("Send FEEDBACK");
						}
						//Si on ne rentre pas dans le catch (format json valide)
						if(cntxt.GetIsOK() && json!=null ){
							//On met le bitset de la tache correspondant à true
							jobs.get(mapAssociatedJobId.get(json.get("JobId"))).getTasks().setBitSet(json.get("Task").asInt());
							//TODO on écrit la réponse dans le fichier correspondant

							//On répond OK au client
							ByteBuffer feedback = UTF8_CHARSET.encode(generateOKAnswer());
							out.put(feedback);
							cntxt.setGetIsOK(false);
						}
					}
				}
			}
		}

		System.out.println("fin process");
		in.compact();
	}


	private void updateInterestOps(SelectionKey key) {

		Context cntxt = (Context) key.attachment();
		int ops = 0;
		ByteBuffer in = cntxt.getIn();
		ByteBuffer out = cntxt.getOut();
		if (out.position() != 0) {
			System.out.println("JE SUIS EN WRITE");
			ops = ops | SelectionKey.OP_WRITE;
		}

		if (in.hasRemaining() && !cntxt.getInputClosed()) {
			System.out.println("JE SUIS EN READ");
			ops = ops | SelectionKey.OP_READ;
		}
		if (ops == 0) {

			silentlyClose(cntxt.getSc());
			return;
		}
		key.interestOps(ops);
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

		Server server = new Server(Integer.parseInt(args[0]),"./src/JarRetConfig.json");
		server.launch();

	}

}
