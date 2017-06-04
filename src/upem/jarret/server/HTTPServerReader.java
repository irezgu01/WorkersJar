package upem.jarret.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import upem.jarret.client.HTTPException;
import upem.jarret.client.HTTPHeader;

public class HTTPServerReader {
	private final Charset UTF8_CHARSET = Server.UTF8_CHARSET;
	private final long TIME_OUT = Server.TIMEOUT;
	public static final String INCOMPLETE_LINE = "incomplete";
	private final SocketChannel sc;
	private final ByteBuffer buff;
	private ByteBuffer currentAnswer;

	public HTTPServerReader(SocketChannel sc, ByteBuffer buff) {
		this.sc = sc;
		this.buff = buff;
	}

	public String readLineCRLF() throws IOException {
		StringBuilder line = new StringBuilder();
		boolean readCr = false;
		buff.flip();
		char b;
		System.out.println(UTF8_CHARSET.decode(buff));
		buff.flip();
		while(buff.hasRemaining() ){
			b = (char) buff.get();
			line.append(b);
			if (b == '\n' && readCr) {
				line.setLength(line.length() - 2);
				buff.compact();
				return line.toString();
			}
			readCr = (b == '\r');
		}
		buff.compact();
		if (sc.read(buff) == -1) {
			throw new HTTPException("serveur ne respecte pas le protocole");
		}
		return INCOMPLETE_LINE;
	}
	

	/**
	 * @return The HTTPHeader object corresponding to the header read
	 * @throws IOException
	 *             HTTPException if the connection is closed before a header
	 *             could be read if the header is ill-formed
	 */
	public Optional<HashMap<String, String>> readHeader() throws IOException {
		String line;
		HashMap<String, String> map = new HashMap<String, String>();
		String response;
		//	long startTime = System.currentTimeMillis();
		if((response = readLineCRLF()).equals(INCOMPLETE_LINE)){

			return Optional.empty();
		}
		map.put("response", response);
		while (!(line = readLineCRLF()).isEmpty()) {
			System.out.println("LINE ----> "+line);
			if(line.equals(INCOMPLETE_LINE)){
				return Optional.empty();
			}
			String[] tokens = line.split(":");
			map.put(tokens[0], tokens[1]);
		}
		System.out.println("END Header");
		return Optional.of(map);
	}


	public Optional<ByteBuffer> readBytes(int size) throws IOException {
			ByteBuffer buffer = ByteBuffer.allocate(size);
			buff.flip();
			buffer.put(buff);
			buff.compact();
			if(buffer.hasRemaining()){
				if(sc.read(buff) == -1){
					throw new HTTPException("serveur ne respecte pas le protocole");
				}
				return Optional.empty();
			}
			
			
		return Optional.of(buffer);
	}

}
