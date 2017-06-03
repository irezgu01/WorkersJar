package upem.jarret.server;

import java.io.IOException;
import java.nio.ByteBuffer;

public class HTTPServerReader {
	
	private final ByteBuffer in;
	private StringBuilder currentLine;
	private ByteBuffer currentAnswer;
//	private final SocketChannel sc;
//    private final ByteBuffer buff;
	
	public HTTPServerReader(ByteBuffer buff, ByteBuffer in) {
//		this.sc = sc;
//        this.buff = buff;
		this.in = in;
		currentLine = new StringBuilder();
	}
	public String readLineCRLF() throws IOException {
		in.flip();

		boolean lastChar = false;
		StringBuilder line = new StringBuilder();
		do{
			if(!in.hasRemaining()) {
				in.clear();
				currentLine.append(line);
				throw new IllegalStateException();
			}
			char b = (char)in.get();
			if(b == '\n' && lastChar) {
				in.compact();
				String res = currentLine.append(line).toString();
				currentLine = new StringBuilder();
				return res;
			}
			if(b == '\r') {
				lastChar = true;
			}
			else {
				if(lastChar) {
					line.append("\r");
				}
				lastChar = false;
				line.append(b);
			}
		} while(true);
	}
	public ByteBuffer readBytes(int size) throws IOException {
		if(currentAnswer == null || currentAnswer.capacity() != size) {
			currentAnswer = ByteBuffer.allocate(size);
		}
		in.flip();
		for(int i=0;i<size;i++) {	
			currentAnswer.put(in.get());
		}
		if(currentAnswer.position() != currentAnswer.capacity()) {
			in.clear();
			throw new IllegalStateException();
		}
		
		return currentAnswer;
	}
}
