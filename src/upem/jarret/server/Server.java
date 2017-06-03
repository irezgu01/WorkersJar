package upem.jarret.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import com.fasterxml.jackson.databind.ObjectMapper;
import upem.jarret.json.Job;
import upem.jarret.json.JsonManipulation;

public class Server {
	private static class Context {
		private HTTPServerReader reader;
		private boolean requestingTask = false;
		private boolean sendingPost = false;
		private boolean readingRequest = true;
		private boolean parsingRequest = false;
		private boolean readingAnswer = false;
		private String request = null;
		private String answer = null;
		private final ByteBuffer bufferInput = ByteBuffer.allocate(BUF_SIZE);;
		private int contentLength;

		public Context(SocketChannel sc) {
			reader = new HTTPServerReader(ByteBuffer.allocate(BUF_SIZE), bufferInput);
		}

		public boolean isRequestingTask() {
			return requestingTask;
		}

		void setRequestingTask(boolean requestingTask) {
			this.requestingTask = requestingTask;
		}
		
		public boolean isSendingPost() {
			return sendingPost;
		}

		private void setSendingPost(boolean sendingPost) {
			this.sendingPost = sendingPost;
		}
		public boolean isReadingRequest() {
			return readingRequest;
		}

		public void setReadingRequest(boolean b) {
			readingRequest = b;
		}
		public void requestAnswer(String answer) {
			this.setAnswer(answer);
			setSendingPost(true);
		}

		public HTTPServerReader getReader() {
			return reader;
		}

		public String getAnswer() {
			return answer;
		}

		private void setAnswer(String answer) {
			this.answer = answer;
		}

		public void clean(SocketChannel sc) {
			setSendingPost(false);
			bufferInput.clear();
			reader = new HTTPServerReader(ByteBuffer.allocate(BUF_SIZE), bufferInput);
		}

		public ByteBuffer getBufferInput() {
			return bufferInput;
		}

		public String getRequest() {
			return request;
		}

		public void setRequest(String request) {
			this.request = request;
		}

		public boolean isParsingRequest() {
			return parsingRequest;
		}

		public void setParsingRequest(boolean b) {
			parsingRequest = b;
		}

		public boolean isReadingAnswer() {
			return readingAnswer;
		}

		public void setReadingAnswer(boolean b) {
			this.readingAnswer = b;
		}

		public int getContentLength() {
			return contentLength;
		}

		public void setContentLength(int length) {
			contentLength = length;
		}
	}

	private static final int BUF_SIZE = 4096;
	//private static final long TIMEOUT = 5_000;
	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private final Set<SelectionKey> selectedKeys;
	private final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(15);

	//private boolean stopAccept = false;
	//private final ObjectMapper mapper = new ObjectMapper();
	private final ServerConfig serverConfig;
	private final Map<Job, JobInfos> jobs;
	private final ArrayList<Job> jobsTasks = new ArrayList<>();

	private final int arraySize;
	private int currentIndex = 0;

	private int currentIndexArrayJobsTasks = 0;
	//private boolean noTaskNow = false;

	public Server(int port, String file) throws Exception {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		selector = Selector.open();
		selectedKeys = selector.selectedKeys();
		serverConfig = JsonManipulation.parseServerConfig(file);
		jobs = JobManager.init(serverConfig.getAnswersDirectory(), serverConfig.getLogDirectory());
		System.out.println(serverConfig);
		initJobsTasks();
		arraySize = jobs.keySet().size();

		jobs.values().forEach(jobInfo -> System.out.println("AnswersDirectory : " + jobInfo.getFileContainsResponses()
				+ "\n" + "LogsDirectory : " + jobInfo.getFileContainsLog()));

	}

	private void initJobsTasks() {
		jobs.keySet().forEach(job -> jobsTasks.add(job));
	}

	private boolean verifyAvailableTask() {
		for (JobInfos info : jobs.values()) {
			if (!info.verifyDoneAllTasks()) {
				return true;
			}
		}
		return false;
	}

	public Optional<String> getTask() {

		Optional<String> result = Optional.empty();
		currentIndexArrayJobsTasks %= arraySize;

		Job job = jobsTasks.get(currentIndexArrayJobsTasks);
		if (jobs.get(job).verifyDoneAllTasks()) {
			currentIndexArrayJobsTasks++;
			currentIndex = 0;
			return getTask();
		}
		if (jobs.get(job).getTasks().hasNext()) {
			result = jobs.get(job).getTasks().nextTask(); // On renvoie la
															// prochaine tâche
															// pas encore
															// acquitée
			currentIndex++;

			if (!jobs.get(job).getTasks().hasNext()) {
				jobs.get(job).setStatusOfDoneAllTasks();

			}

		} else {
			jobs.get(job).setStatusOfDoneAllTasks(); // Si pas de suivant alors
														// toutes les tâches ont
														// été traitées
		}
		if (currentIndex == job.getJobPriority()) {
			// On passe aux tâches des jobs suivants
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
		ByteBuffer response = UTF8_CHARSET.encode(body.toString());
		int length = response.remaining();

		header.append("HTTP/1.1 200 OK\r\nContent-type: application/json; charset=utf-8\r\nContent-length: ")
				.append(length).append("\r\n\r\n");
		ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);
		buffer.put(UTF8_CHARSET.encode(header.toString()));
		buffer.put(response);
		return buffer;
	}

	private String generateErrorAnswer() {
		StringBuilder httpError = new StringBuilder();
		httpError.append("HTTP/1.1 400").append("\r\n\r\n");

		return httpError.toString();
	}

	public void launch() throws IOException {
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		Set<SelectionKey> selectedKeys = selector.selectedKeys();
		
		
		while (!Thread.interrupted()) {
			if (verifyAvailableTask()) {
				System.out.println(getTask().toString());
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// selector.select();
			// processSelectedKeys();
			// selectedKeys.clear();
		}
	}

	private void processSelectedKeys() throws IOException {
		for (SelectionKey key : selectedKeys) {

			if (key.isValid() && key.isAcceptable()) {
				try {
					doAccept(key);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (key.isValid() && key.isWritable()) {
				try {
					doWrite(key);
				} catch (IOException e) {
					SocketChannel sc = (SocketChannel) key.channel();
					//saveLog("Connection lost with client " + sc.getRemoteAddress());
					close(key);
				}
			}
			if (key.isValid() && key.isReadable()) {
				try {
					doRead(key);
				} catch (IOException e) {
					SocketChannel sc = (SocketChannel) key.channel();
					//saveLog("Connection lost with client " + sc.getRemoteAddress());
					close(key);
				}
			}
		}
	}

	private void doAccept(SelectionKey key) throws IOException {
		SocketChannel sc = serverSocketChannel.accept();
		if (sc == null) {
			return;
		}
		sc.configureBlocking(false);
		sc.register(selector, SelectionKey.OP_READ, new Context(sc));
		//saveLog("New connection from " + sc.getRemoteAddress());
	}
	private void doRead(SelectionKey key) throws IOException {
		SocketChannel sc = (SocketChannel) key.channel();
		Context context = (Context) key.attachment();
		HTTPServerReader reader = context.getReader();

		sc.read(context.getBufferInput());

		if (context.isReadingRequest()) {
			try {
				context.setRequest(reader.readLineCRLF());
				context.setReadingRequest(false);
			} catch (IllegalStateException e) {
				return;
			}
		}

		try {
			parseRequest(context, sc);
		} catch (IllegalStateException e) {
			// System.out.println("resuqesting task !");
			return;
		} catch (Exception e) {
			//sc.write(charsetUTF8.encode(badRequest));
			return;
		}

		key.interestOps(SelectionKey.OP_WRITE);
	}
	private void parseRequest(Context context, SocketChannel sc) throws IOException {
		String request = context.getRequest();
		String firstLine = request.split("\r\n")[0];
		String[] token = firstLine.split(" ");
		String cmd = token[0];
		String requested = token[1];
		String protocol = token[2];

		if (cmd.equals("GET") && requested.equals("Task") && protocol.equals("HTTP/1.1")) {
			if (!context.isParsingRequest()) {
				//saveLog("Client " + sc.getRemoteAddress() + " is requesting a task");
			}
			context.setRequestingTask(true);
			context.setParsingRequest(true);
			if (context.isParsingRequest()) {
				while (!context.getReader().readLineCRLF().equals("")) {
					/** read useless parameters og GET request **/
				}
				context.setParsingRequest(false);
			}
		} else if (cmd.equals("POST") && requested.equals("Answer") && protocol.equals("HTTP/1.1")) {
			if (!context.isParsingRequest()) {
				//saveLog("Client " + sc.getRemoteAddress() + " is posting an answer");
			}
			context.setParsingRequest(true);
			String answer = parsePOST(context);
			Objects.requireNonNull(answer);
			context.requestAnswer(answer);
			context.setParsingRequest(false);
		} else {
			throw new IllegalArgumentException();
		}
	}
	private String parsePOST(Context context) throws IOException {
		HTTPServerReader reader = context.getReader();
		if (!context.isReadingAnswer()) {
			String line;
			while (!(line = reader.readLineCRLF()).equals("")) {
				String[] token = line.split(": ");
				if (token[0].equals("Content-Length")) {
					context.setContentLength(Integer.parseInt(token[1]));
				}
				if (token[0].equals("Content-Type")) {
					if (!token[1].equals("application/json")) {
						throw new IllegalArgumentException();
					}
				}
			}
			context.setReadingAnswer(true);
		}
		ByteBuffer bb = reader.readBytes(context.getContentLength());
		context.setReadingAnswer(false);
		bb.flip();
		long jobId = bb.getLong();
		int task = bb.getInt();
		String answer = UTF8_CHARSET.decode(bb).toString();
		System.out.println(answer);
		if (answer != null && JsonManipulation.isJson(answer)) {
			saveAnswer(jobId, task, answer);
			//nbAnswers++;
		}

		return answer;
	}
	private void saveAnswer(long jobId, int task, String answer) throws IOException {
		int fileNumber = 1;
		long size = 0;
		Path answerFilePath;
		answer += '\n';

		do {
			answerFilePath = Paths.get(answersPath + jobId + "_" + fileNumber++);
			if (Files.exists(answerFilePath, LinkOption.NOFOLLOW_LINKS)) {
				size = Files.size(answerFilePath);
			} else {
				break;
			}
		} while (size > maxFileSize);

		try (BufferedWriter writer = Files.newBufferedWriter(answerFilePath, StandardOpenOption.APPEND,
				StandardOpenOption.CREATE); PrintWriter out = new PrintWriter(writer)) {
			out.println(answer);
		} catch (IOException e) {
			System.err.println(e);
		}
	}
	
	private void doWrite(SelectionKey key) throws IOException {
		Context context = (Context) key.attachment();

		if (context.isRequestingTask()) {
			context.setRequestingTask(false);
			sendTask((SocketChannel) key.channel());
			key.interestOps(SelectionKey.OP_READ);
		} else if (context.isSendingPost()) {
			sendCheckCode(key);
			key.interestOps(SelectionKey.OP_READ);
		}

		context.setReadingRequest(true);
	}
	private void sendTask(SocketChannel sc) throws IOException {
		Job job = jobs.poll();
		ByteBuffer jsonBuffer;
		if (job == null) {
			jsonBuffer = UTF8_CHARSET.encode("\"ComeBackInSeconds\":" + comeBackInSeconds);
		} else {
			while (job.isFinished()) {
				job = jobs.poll();
			}
			String json = job.nextTask().toJSON();
			jsonBuffer = Server.charsetUTF8.encode(json);
		}

		String header = "HTTP/1.1 200 OK\r\n" + "Content-Type: application/json; charset=utf-8\r\n" + "Content-Length: "
				+ jsonBuffer.remaining() + "\r\n\r\n";
		ByteBuffer headerBuffer = Server.charsetUTF8.encode(header);

		while (headerBuffer.hasRemaining()) {
			sc.write(headerBuffer);
		}

		while (jsonBuffer.hasRemaining()) {
			sc.write(jsonBuffer);
		}
		if (!job.isFinished()) {
			jobs.addLast(job);
		}
	}
	private void sendCheckCode(SelectionKey key) throws IOException {
		Context context = (Context) key.attachment();
		SocketChannel sc = (SocketChannel) key.channel();
		String answer = context.getAnswer();
		if (answer == null) {
			throw new IllegalArgumentException("No answer");
		}
		if (JsonManipulation.isJson(answer)) {
			sc.write(UTF8_CHARSET.encode(HTTP_1_1_200_OK));
		} else {
			sc.write(UTF8_CHARSET.encode(badRequest));
		}

		context.clean(sc);
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
		Server server = new Server(7777);
		server.launch();

	}

}