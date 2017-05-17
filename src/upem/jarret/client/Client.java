package upem.jarret.client;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
;


public class Client {
	private final InetSocketAddress server ;
	private final SocketChannel sc ;
	private final String clientID;
	private HTTPReader httpReader;
	private final ByteBuffer buffer;
	private static final int BUFFER_SIZE = 1024; 
	private final Charset ASCII_CHARSET = Charset.forName("ASCII");

	
	public Client(String address, String cliendID,int port ) throws IOException {
	
		server = new InetSocketAddress(address, port);
		this.clientID = cliendID;
		sc = SocketChannel.open();
		System.out.println("avant connexion");
		sc.connect(server);
		System.out.println("après connexion");
		buffer = ByteBuffer.allocate(BUFFER_SIZE);
		httpReader = new HTTPReader(sc, buffer); 
	}
	
	public void createGetRequest() throws IOException{
		String request = "GET Task HTTP/1.1\r\nHost: "+server.getHostName()+"\r\n\r\n";
		sc.write(ASCII_CHARSET.encode(request));
	}
	
	public void createPostResponse(int length,String response) throws IOException{
		String postResponse = "POST Answer HTTP/1.1\r\nHost: "+server.getHostName()+" Content-Type: application/json\r\nContent-Length: "
								+length+"\r\n\r\n"+response;
		sc.write(ASCII_CHARSET.encode(postResponse));
		
	}
	
	public void getResponse() throws IOException{
		HTTPHeader header = httpReader.readHeader();
		String workerURL = header.getFields().get("WorkerURL");
		String WorkerClassName = header.getFields().get("WorkerClassName");
		
		System.out.println(header);
	}
	public static void main(String[] args) throws IOException {
		System.out.println("sdfffffffffffffffff");
		Client client = new Client("ns3001004.ip-5-196-73.eu", "1", 8080);
		System.out.println("ooooups");
		client.createGetRequest();
		client.getResponse();
	}
	
	
}