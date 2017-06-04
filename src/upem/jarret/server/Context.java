package upem.jarret.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class Context {
	//private static final int BUF_SIZE = 4096;
	private boolean inputClosed = false;
	
	private final ByteBuffer in = ByteBuffer.allocate(Server.BUF_SIZE);
	private final ByteBuffer out = ByteBuffer.allocate(Server.BUF_SIZE);
	private final SelectionKey key;
	private final SocketChannel sc;
	private final HTTPServerReader reader;
	private boolean getIsOK;

	public Context(SelectionKey key) {
		this.key = key;
		this.sc = (SocketChannel) key.channel();
		this.reader = new HTTPServerReader(sc, in);
		getIsOK = false;
	}
	public boolean GetIsOK() {
		return getIsOK;
	}

	public void setGetIsOK(boolean getIsOK) {
		this.getIsOK = getIsOK;
	}

	public boolean getInputClosed() {
		return inputClosed;
	}
	public void setInputClosed(boolean inputClosed) {
		this.inputClosed = inputClosed;
	}

	public ByteBuffer getIn() {
		return in;
	}

	public ByteBuffer getOut() {
		return out;
	}

	public SelectionKey getKey() {
		return key;
	}

	public SocketChannel getSc() {
		return sc;
	}

	public HTTPServerReader getReader() {
		return reader;
	}
	public int read() throws IOException{
		return sc.read(in);
	}
	public void write() throws IOException{
		out.flip();
	//	System.out.println("Bytes à ecrire "+Charset.forName("UTF-8").decode(out));
		out.flip();
		int r = sc.write(out);
	//	System.out.println("Bytes écris "+r);
		out.compact();
	}

	

}
