package upem.jarret.client;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import upem.jarret.json.JsonManipulation;
import upem.jarret.worker.Worker;
import upem.jarret.worker.WorkerFactory;

public class Client {
	private final InetSocketAddress server;
	private SocketChannel sc;
	private final String clientID;
	private HTTPReader httpReader;
	private HTTPHeader header;
	private String content;
	private ByteBuffer buffer;
	private static final int BUFFER_SIZE = 4096;
	private final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	private LinkedList<Worker> workers = new LinkedList<>();


	public Client(String address, int port, String cliendID) {
		server = new InetSocketAddress(address, port);
		this.clientID = cliendID;
		buffer = ByteBuffer.allocate(BUFFER_SIZE);
	}

	/**
	 * create the body of the get request and send it to the server
	 * @throws IOException
	 */
	public void sendGetRequest() throws IOException  {
		sc = SocketChannel.open();
		sc.connect(server);
		httpReader = new HTTPReader(sc, buffer);
		StringBuilder request = new StringBuilder();
		request.append("GET Task HTTP/1.1\r\nHost: ").append(server.getHostName()).append("\r\n\r\n");
		try {
			sc.write(UTF8_CHARSET.encode(request.toString()));
		} catch (IOException e) {
			sendGetRequest(); //On retente de se reconnecter et de recuperer une tâche
		}

	}

	/**
	 * create the body of the response (post request) and send it to the server
	 * @param length
	 *            the length of the body of the request
	 * @param response
	 *            the answer of the client (jsonNode)
	 * @throws IOException
	 */
	public void sendPostResponse(ObjectNode response) throws IOException  {
		long jobId = response.get("JobId").asLong();
		int task = response.get("Task").asInt();
		ByteBuffer responsebb = UTF8_CHARSET.encode(response.toString());
		int length = responsebb.remaining()+ Long.BYTES + Integer.BYTES;
		StringBuilder httpPostHeader = new StringBuilder();
		httpPostHeader.append("POST Answer HTTP/1.1\r\nHost: ").append(server.getHostName())
		.append(" Content-Type: application/json\r\nContent-Length: ").append(length).append("\r\n\r\n");
		System.out.println("Writing answer to server");
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		buffer.put(UTF8_CHARSET.encode(httpPostHeader.toString()));
		buffer.putLong(jobId);
		buffer.putInt(task);
		buffer.put(responsebb);
		buffer.flip();
		try {
			sc.write(buffer);
		} catch (IOException e) {
			
			
			//On retente de se reconnecter et de renvoyer la réponse

			try {
				sc.connect(server);
				sc.write(buffer);
			} catch (IOException e1) {
				sendGetRequest();			//En cas d'échec on réitère le processus en demandant à nouveau une tâche 
			}
			
		}




	}

	/**
	 * read the body of the response of the server
	 * @param header
	 *            the head of the response received from the server
	 * @return String the task received from the server
	 * @throws IOException
	 */
	public String readContent(HTTPHeader header) throws IOException {
		int contentLength = header.getContentLength();
		ByteBuffer content;
		if (contentLength != -1) {
			content = httpReader.readBytes(header.getContentLength());
		} else {
			content = httpReader.readChunks();
		}
		content.flip();
		String response = header.getCharset().decode(content).toString();
		return response;
	}

	/**
	 * get the response from the server. Separate the header from the body
	 * @return Optional<String> the task received from the server. Empty if the
	 *         server sends a bad response
	 * @throws IOException
	 */
	public Optional<String> getResponse() throws IOException {
		header = httpReader.readHeader();
		if (header.getCode() == 200) {
			if (!header.getContentType().equals("application/json")) {
				System.out.println("serve error : response is not JSON");
				return Optional.empty();
			}
			content = readContent(header);
			return Optional.of(content);

		}
		return Optional.empty();

	}

	public String getResponseCodeServer() throws IOException {
		header = httpReader.readHeader();
		StringBuilder result = new StringBuilder("Responding of server:\n");
		result.append(header.getResponse());
		sc.close();
		return result.toString();

	}

	/**
	 * The client sleeps for x seconds
	 * @param serverTask
	 * @throws InterruptedException
	 */
	private void sleep(ObjectNode serverTask) throws InterruptedException {
		long timeToSleep = serverTask.get("ComeBackInSeconds").asLong() * 1_000;
		System.out.println("sleeping ...");
		Thread.sleep(timeToSleep);
	}

	/**
	 * si le worker existe déja elle le renvoie sinon elle crée un nouveau
	 * @param serverTask
	 * @return
	 * @throws MalformedURLException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private Worker getWorker(ObjectNode serverTask)
			throws MalformedURLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		long jobId = serverTask.get("JobId").asLong();
		String workerVersion = serverTask.get("WorkerVersion").asText();

		Worker worker = workers.stream()
				.filter(myworker -> myworker.getJobId() == jobId && myworker.getVersion().equals(workerVersion))
				.findAny().orElse(null);
		if (worker != null) {
			return worker;
		} else {
			String workerUrl = serverTask.get("WorkerURL").asText();
			String workerClass = serverTask.get("WorkerClassName").asText();
			worker = WorkerFactory.getWorker(workerUrl, workerClass);
			workers.add(worker);
		}
		return worker;
	}

	/**
	 * vérifier si le résultat du compute est un json ou pas
	 * @param computation
	 * @return
	 */
	private boolean isNotValidJson(String computation) {
		try {
			final ObjectMapper mapper = new ObjectMapper();
			mapper.readTree(computation);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * creer une réponse d'erreur
	 * @param error
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public ObjectNode createErrorResponse(String error) throws JsonProcessingException, IOException {
		ObjectNode computationError = JsonManipulation.tojson(content);
		computationError.put("ClientId", clientID);
		computationError.put("Error", error);
		return computationError;
	}

	/**
	 * creer la bonne réponse à renvoyer au serveur
	 * @param computation
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public ObjectNode createGoodResponse(ObjectNode computation) throws JsonProcessingException, IOException {
		ObjectNode response = JsonManipulation.tojson(content);
		response.put("ClientId", clientID);
		response.set("Answer", computation);
		return response;
	}

	/**
	 * execute le compute du worker
	 * 
	 * @param serverTask
	 * @return le résultat du compute ObjectNode
	 * @throws JsonProcessingException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public ObjectNode compute(ObjectNode serverTask) throws JsonProcessingException, IOException,
	ClassNotFoundException, IllegalAccessException, InstantiationException {
		System.out.println("Retrieving worker");
		Worker worker = getWorker(serverTask);
		int task = serverTask.get("Task").asInt();
		System.out.println("Starting computation");
		String computation = worker.compute(task);
		if (computation == null) {
			return createErrorResponse("Computation error");
		}
		if (!isNotValidJson(computation)) {
			return createErrorResponse("Answer is not valid JSON");
		}
		return JsonManipulation.tojson(computation);
	}

	/**
	 * Vérifie que le résultat du compute n'est pas trop long
	 * @param computation
	 * @return
	 * @throws HTTPException
	 */
	public boolean isTooLong(ObjectNode computation) throws HTTPException {
		return Integer.BYTES+Long.BYTES+UTF8_CHARSET.encode(computation.toString()).remaining() + header.getContentLength() > 4096;
	}

	/**
	 * vérifie que le résultat du compute ne contient pas d'object
	 * @param objectNode
	 * @return
	 */
	public boolean isNested(ObjectNode objectNode) {
		Iterator<JsonNode> iterator = objectNode.elements();
		while (iterator.hasNext()) {
			JsonNode node = iterator.next();
			if (node.getNodeType() == JsonNodeType.OBJECT) {
				return true;
			}
		}
		return false;
	}

	public static void usage() {
		System.out.println("Usage : Client serverAddress port clientID");
	}

	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException,
	IllegalAccessException, InstantiationException {
		if (args.length != 3) {
			usage();
			return;
		}
		Client client = new Client(args[0], Integer.valueOf(args[1]), args[2]);
		while (!Thread.interrupted()) {
			client.sendGetRequest();
			Optional<String> content = client.getResponse();
			if (content.isPresent()) {
				ObjectNode serverTask = JsonManipulation.tojson(content.get().toString());
				if (serverTask.get("ComeBackInSeconds") != null) {
					client.sleep(serverTask);
				} else {
					System.out.println(JsonManipulation.jsonString(serverTask));
					ObjectNode computation = client.compute(serverTask);
					if (client.isTooLong(computation)) {
						ObjectNode response = client.createErrorResponse("Too Long");
						client.sendPostResponse(response);
					} else if (client.isNested(computation)) {
						ObjectNode response = client.createErrorResponse("Answer is nested");
						client.sendPostResponse(response);
					} else {
						ObjectNode response = client.createGoodResponse(computation);
						client.sendPostResponse(response);
						System.out.println("\n***************************************\n");
						System.out.println(client.getResponseCodeServer());
						System.out.println("\n***************************************\n");

					}

				}
			} else {
				System.out.println("Bad response");
			}
		}
	}


}
