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

import upem.jarret.worker.Worker;
import upem.jarret.worker.WorkerFactory;

public class Client {
	private final InetSocketAddress server;
	private final SocketChannel sc;
	private final String clientID;
	private HTTPReader httpReader;
	private HTTPHeader header;
	private String content;
	private final ByteBuffer buffer;
	private static final int BUFFER_SIZE = 4096;
	private final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	private LinkedList<Worker> workers = new LinkedList<>();

	public Client(String address, String cliendID, int port) throws IOException {

		server = new InetSocketAddress(address, port);
		this.clientID = cliendID;
		sc = SocketChannel.open();
		System.out.println("trying to connect ...");
		sc.connect(server);
		System.out.println("connected");
		buffer = ByteBuffer.allocate(BUFFER_SIZE);
		httpReader = new HTTPReader(sc, buffer);
	}

	/**
	 * create the body of the get request and send it to the server
	 * 
	 * @throws IOException
	 */
	public void sendGetRequest() throws IOException {
		//a changer en string builder
		String request = "GET Task HTTP/1.1\r\nHost: " + server.getHostName() + "\r\n\r\n";
		sc.write(UTF8_CHARSET.encode(request));
	}

	/**
	 * create the body of the response (post request) and send it to the server
	 * 
	 * @param length
	 *            the length of the body of the request
	 * @param response
	 *            the answer of the client
	 * @throws IOException
	 */
	public void sendPostResponse(int length, ObjectNode response) throws IOException {
		long jobId = response.get("JobId").asLong();
		int task = response.get("Task").asInt();
		//a changer en stringBuilder
		String HeaderResponse = "POST Answer HTTP/1.1\r\nHost: " + server.getHostName()
				+ " Content-Type: application/json\r\nContent-Length: " + length + "\r\n\r\n" ;
		String contentResponse = response.textValue();
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		buffer.put(UTF8_CHARSET.encode(HeaderResponse));
		buffer.putLong(jobId);
		buffer.putInt(task);
		buffer.put(UTF8_CHARSET.encode(contentResponse));
		buffer.flip();
		sc.write(buffer);
		sc.close();

	}

	/**
	 * convert the string received from the server to a ObjectNode
	 * 
	 * @param string
	 *            the body of the task received from the server
	 * @return ObjectNode the response of the server
	 * @throws IOException 
	 * @throws JsonProcessingException 
	 */
	private ObjectNode tojson(String stringResponse) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(stringResponse);
		ObjectNode objectNode = (ObjectNode) node;
		return objectNode;
	}

	/**
	 * read the body of the response of the server
	 * 
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
	 * 
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
	/**
	 * The client sleeps for x seconds 
	 * @param serverTask 
	 * @throws InterruptedException
	 */
	private void sleep(ObjectNode serverTask) throws InterruptedException {
		long timeToSleep = serverTask.get("ComeBackInSeconds").asLong()*10;
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
	private Worker getWorker(ObjectNode serverTask) throws MalformedURLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		long jobId = serverTask.get("JobId").asLong();
		String workerVersion = serverTask.get("WorkerVersion").asText();
		
		Worker worker = workers.stream().filter(myworker -> myworker.getJobId() == jobId && myworker.getVersion().equals(workerVersion)).findAny().orElse(null);
		if (worker != null) {
			return worker;
		} else {
				String workerUrl = serverTask.get("WorkerURL").asText();
				String workerClass = serverTask.get("WorkerClassName").asText();
				worker = WorkerFactory.getWorker(workerUrl,workerClass);
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
		ObjectNode computationError = tojson(content);
		computationError.put("ClientId",clientID);
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
		ObjectNode response = tojson(content);
		response.put("ClientId",clientID);
		response.set("Answer", computation);
		return response;
	}
	/**
	 * récupérer le résultat du compute du worker
	 * @param serverTask
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public ObjectNode compute(ObjectNode serverTask) throws JsonProcessingException, IOException, ClassNotFoundException, IllegalAccessException, InstantiationException{
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
		return tojson(computation);
	}
	/**
	 * creer une réponse error too long
	 * @param computation
	 * @return
	 * @throws HTTPException
	 */
	public boolean isTooLong(ObjectNode computation) throws HTTPException {
		return UTF8_CHARSET.encode(computation.asText()).position()+ header.getContentLength() < 4096;
	}
	/**
	 * creer une reponse d'erreur is nested
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
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		Client client = new Client("ns3001004.ip-5-196-73.eu", "1", 8080);
		System.out.println("create get request");
		client.sendGetRequest();
		System.out.println("getting the response ...");
		Optional<String> content = client.getResponse();
		if (content.isPresent()) {
			ObjectNode serverTask = client.tojson(content.get().toString());
			if (serverTask.get("ComeBackInSeconds") != null) {
				client.sleep(serverTask);
			}
			else{
				System.out.println(serverTask);
				ObjectNode computation = client.compute(serverTask);
				if(client.isTooLong(computation)) {
					ObjectNode response = client.createErrorResponse("Too Long");
					client.sendPostResponse(response.toString().getBytes().length, response);
				}
				else if(client.isNested(computation)) {
					ObjectNode response = client.createErrorResponse("Answer is nested");
					client.sendPostResponse(response.toString().getBytes().length, response);
				}
				else {
					ObjectNode response = client.createGoodResponse(computation);
					client.sendPostResponse(computation.toString().getBytes().length, response);
				}
				
			}
		}
	}

}