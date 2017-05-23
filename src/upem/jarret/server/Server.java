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
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Server {
	private static class Context {
		private boolean inputClosed = false;
		private final ByteBuffer in = ByteBuffer.allocate(BUF_SIZE);
		private final ByteBuffer out = ByteBuffer.allocate(BUF_SIZE);
		private final SelectionKey key;
		private final SocketChannel sc;

		public Context(SelectionKey key) {
			this.key = key;
			this.sc = (SocketChannel) key.channel();
		}

		public void doRead() throws IOException {
			if (sc.read(in) == -1) {
				inputClosed = true;
			}
			process();
			updateInterestOps();
		}

		public void doWrite() throws IOException {
			out.flip();
			sc.write(out);
			out.compact();
			process();
			updateInterestOps();
		}

		private void process() {
			in.flip();

			while (in.remaining() >= 2 * Integer.BYTES && out.remaining() >= Integer.BYTES) {
				out.putInt(in.getInt() + in.getInt());
			}
			in.compact();
		}

		private void updateInterestOps() {

			int ops = 0;

			if (out.position() != 0) {
				ops = ops | SelectionKey.OP_WRITE;
			}

			if (in.hasRemaining() && !inputClosed) {
				ops = ops | SelectionKey.OP_READ;
			}
			if (ops == 0) {

				silentlyClose(sc);
				return;
			}
			key.interestOps(ops);
		}

	}

	private static final int BUF_SIZE = 4096;
	private static final long TIMEOUT = 5_000;
	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;
	private final Set<SelectionKey> selectedKeys;
	private final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(4);
	private boolean stopAccept = false;
	ObjectMapper mapper = new ObjectMapper();

	public Server(int port) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		selector = Selector.open();
		selectedKeys = selector.selectedKeys();
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

	public void launch() throws IOException {
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		Set<SelectionKey> selectedKeys = selector.selectedKeys();
		while (!Thread.interrupted()) {

			selectedKeys.clear();
		}
	}

	private void processSelectedKeys() throws IOException {
		for (SelectionKey key : selectedKeys) {
			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
			}
			try {
				Context cntxt = (Context) key.attachment();
				if (key.isValid() && key.isWritable()) {
					cntxt.doWrite();
				}
				if (key.isValid() && key.isReadable()) {
					cntxt.doRead();
				}
			} catch (IOException e) {
				silentlyClose(key.channel());
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

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			usage();
			return;
		}
		Server server = new Server(7777);

	}

}
