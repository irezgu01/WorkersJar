package upem.jarret.client;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;

public class HTTPReader {

	private final Charset ASCII_CHARSET = Charset.forName("ASCII");
	private final SocketChannel sc;
	private final ByteBuffer buff;

	public HTTPReader(SocketChannel sc, ByteBuffer buff) {
		this.sc = sc;
		this.buff = buff;
	}

	/**
	 * @return The ASCII string terminated by CRLF
	 *         <p>
	 *         The method assume that buff is in write mode and leave it in
	 *         write-mode The method never reads from the socket as long as the
	 *         buffer is not empty
	 * @throws IOException
	 *             HTTPException if the connection is closed before a line could
	 *             be read
	 */
	public String readLineCRLF() throws IOException {
		boolean readCr = false;
		StringBuilder line = new StringBuilder();
		char b;
		while (true) {
			buff.flip();
			while (buff.hasRemaining()) {
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
		}
	}

	/**
	 * @return The HTTPHeader object corresponding to the header read
	 * @throws IOException
	 *             HTTPException if the connection is closed before a header
	 *             could be read if the header is ill-formed
	 */
	public HTTPHeader readHeader() throws IOException {
		String line;
		HashMap<String, String> map = new HashMap<String, String>();
		String response = readLineCRLF();
		while (!(line = readLineCRLF()).isEmpty()) {
			String[] tokens = line.split(":");
			map.put(tokens[0], tokens[1]);
		}
		return HTTPHeader.create(response, map);

	}

	static boolean readFully(ByteBuffer buff, SocketChannel sc) throws IOException {
		while (buff.hasRemaining()) {
			if (sc.read(buff) == -1) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @param size
	 * @return a ByteBuffer in write-mode containing size bytes read on the
	 *         socket
	 * @throws IOException
	 *             HTTPException is the connection is closed before all bytes
	 *             could be read
	 */
	public ByteBuffer readBytes(int size) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(size);
		buff.flip();
		buffer.put(buff);
		buff.compact();
		boolean allread = readFully(buffer, sc);
		if (!allread) {
			throw new HTTPException("serveur ne respecte pas le protocole");
		}
		return buffer;

	}

	/**
	 * @return a ByteBuffer in write-mode containing a content read in chunks
	 *         mode
	 * @throws IOException
	 *             HTTPException if the connection is closed before the end of
	 *             the chunks if chunks are ill-formed
	 */

	public ByteBuffer readChunks() throws IOException {
		LinkedList<ByteBuffer> buffers = new LinkedList<ByteBuffer>();
		int chunk; 

		do {
			chunk = Integer.parseInt(readLineCRLF(), 16);
			if(chunk ==0) {
				break;
			}
			ByteBuffer buffreceive = ByteBuffer.allocate(chunk);
			//System.out.println("remaining : "+buffreceive.remaining());
			buff.flip();
			buffreceive.put(buff);
			buff.compact();
			boolean allread = readFully(buffreceive, sc);
			if (!allread) {
				throw new HTTPException("serveur ne respecte pas le protocole");
			}
			buffreceive.flip();
			buffers.add(buffreceive);
			readLineCRLF();
			//System.out.println(chunk);
		} while(chunk !=0);
		ByteBuffer finalbuffer = ByteBuffer.allocate(buffers.stream().mapToInt(e -> e.remaining()).sum());
		buffers.stream().forEach(finalbuffer::put );
		return finalbuffer;
	}
	
	public static void main(String[] args) throws IOException {
		Charset charsetASCII = Charset.forName("ASCII");
		String request;
		SocketChannel sc;		

		ByteBuffer bb = ByteBuffer.allocate(50);
		request = "GET / HTTP/1.1\r\n" + "Host: www.u-pem.fr\r\n" + "\r\n";
		sc = SocketChannel.open();
		sc.connect(new InetSocketAddress("www.u-pem.fr", 80));
		HTTPReader reader = new HTTPReader(sc, bb);
		sc.write(charsetASCII.encode(request));
		HTTPHeader header = reader.readHeader();
		System.out.println(header);
		ByteBuffer content = reader.readChunks();
		content.flip();
		System.out.println(header.getCharset().decode(content));
		sc.close();
	}
}
