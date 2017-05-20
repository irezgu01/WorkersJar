package upem.jarret.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Client {
	private final InetSocketAddress server;
	private final SocketChannel sc;
	private final String clientID;
	private HTTPReader httpReader;
	private final ByteBuffer buffer;
	private static final int BUFFER_SIZE = 1024;
	private final Charset ASCII_CHARSET = Charset.forName("ASCII");

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
	public void createGetRequest() throws IOException {
		String request = "GET Task HTTP/1.1\r\nHost: " + server.getHostName() + "\r\n\r\n";
		sc.write(ASCII_CHARSET.encode(request));
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
	public void createPostResponse(int length, String response) throws IOException {
		String postResponse = "POST Answer HTTP/1.1\r\nHost: " + server.getHostName()
				+ " Content-Type: application/json\r\nContent-Length: " + length + "\r\n\r\n" + response;
		sc.write(ASCII_CHARSET.encode(postResponse));

	}

	/**
	 * convert the string received from the server to a Task
	 * 
	 * @param string
	 *            the body of the task received from the server
	 * @return Task the response of the server
	 */
	private Task tojson(String string) {
		ObjectMapper mapper = new ObjectMapper();
		Task task = new Task();
		try {
			task = mapper.readValue(string, Task.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return task;
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
	 * @return Optional<Task> the task received from the server. Empty if the
	 *         server sends a bad response
	 * @throws IOException
	 */
	public Optional<Task> getResponse() throws IOException {
		HTTPHeader header = httpReader.readHeader();
		if (header.getCode() == 200) {
			if (!header.getContentType().equals("application/json")) {
				System.out.println("serve error : response is not JSON");
				return Optional.empty();
			}
			String response = readContent(header);
			/*
			 * ByteBuffer content; if(contentLength !=-1) { content =
			 * httpReader.readBytes(header.getContentLength()); } else { content
			 * = httpReader.readChunks(); } content.flip(); String response =
			 * header.getCharset().decode(content).toString();
			 */
			Task task = tojson(response);
			return Optional.of(task);

		}
		System.out.println("*******************header ***************** \n" + header);
		return Optional.empty();

	}

	public static void main(String[] args) throws IOException {
		Client client = new Client("ns3001004.ip-5-196-73.eu", "1", 8080);
		System.out.println("create get request");
		client.createGetRequest();
		System.out.println("getting the response ...");
		Optional<Task> response = client.getResponse();
		if (response.isPresent()) {
			System.out.println("la reponse est : ");
			System.out.println(response.get().toString());
		}
	}

}