package upem.jarret.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Server {
	   private static class Context {
	        private boolean inputClosed = false;
	        private final ByteBuffer in = ByteBuffer.allocate(BUF_SIZE);
	        private final ByteBuffer out = ByteBuffer.allocate(BUF_SIZE);
	        private final SelectionKey key;
	        private final SocketChannel sc;
	        private long timeout;

	        public Context(SelectionKey key) {
	            this.key = key;
	            this.sc = (SocketChannel) key.channel();
	        }

	        public void doRead() throws IOException {
	            if(sc.read(in) == -1){
	                inputClosed=true;
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
	        	
	            while(in.remaining() >= 2 * Integer.BYTES && out.remaining() >= Integer.BYTES){
	        		out.putInt(in.getInt() + in.getInt());
	        	}
	            in.compact();
	        }
	        
	        public void resetInactiveTime(){
	        	this.timeout = 0;
	        }
	        
	        public void addInactiveTime(long time,long timeout){
	        	this.timeout +=time;
	        	if(this.timeout > timeout){
	        		silentlyClose(sc);
	        	}
	        }
	        
	        

	        private void updateInterestOps(){
	 
	    		int ops = 0;

	    		if(out.position() !=0){
	    			ops = ops | SelectionKey.OP_WRITE;
	    		}

	    		if(in.hasRemaining() && !inputClosed){
	    			ops = ops | SelectionKey.OP_READ;
	    		}
	    		if(ops == 0){
	    
	    			silentlyClose(sc);
	    			return;
	    		}
	    		key.interestOps(ops);
	        }

	    }

	    private static final int BUF_SIZE = 512;
		private static final long TIMEOUT = 5_000;
	    private final ServerSocketChannel serverSocketChannel;
	    private final Selector selector;
	    private final Set<SelectionKey> selectedKeys;
	//    private final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1) ;
	    private boolean stopAccept = false;
	    ObjectMapper mapper = new ObjectMapper();

	    public Server(int port) throws IOException {
	        serverSocketChannel = ServerSocketChannel.open();
	        serverSocketChannel.bind(new InetSocketAddress(port));
	        selector = Selector.open();
	        selectedKeys = selector.selectedKeys();
	    }

	    public void launch() throws IOException {
	        serverSocketChannel.configureBlocking(false);
	        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
	        Set<SelectionKey> selectedKeys = selector.selectedKeys();
	        while (!Thread.interrupted()) {
	            
	            long startLoop = System.currentTimeMillis();
	            selector.select(TIMEOUT/10);
	            
	            processSelectedKeys();
	            long endLoop = System.currentTimeMillis();
	            long timeSpent = endLoop-startLoop;
	            updateInactivityKeys(timeSpent);
	            
	            selectedKeys.clear();
	        }
	    }

		private void updateInactivityKeys(long timeSpent) {
	    	for(SelectionKey key : selector.keys()){
	            Context cntxt= (Context) key.attachment();
	            if(cntxt !=null){
	            	cntxt.addInactiveTime(timeSpent,TIMEOUT);
	            }
	    	}	
		}
		private void processSelectedKeys() throws IOException {
			boolean reset;
	        for (SelectionKey key : selectedKeys) {
	        	reset = false;
	            if (key.isValid() && key.isAcceptable()) {
	                doAccept(key);
	            }
	            try {
	                Context cntxt= (Context) key.attachment();
	                if (key.isValid() && key.isWritable()) {
	                    cntxt.doWrite();
	                    reset = true;
	                }
	                if (key.isValid() && key.isReadable()) {
	                    cntxt.doRead();
	                    reset = true;
	                }
	                if(reset){
	                	cntxt.resetInactiveTime();
	                	reset = false;
	                }
	            } catch (IOException e) {
	                silentlyClose(key.channel());
	            }
	        }
	        
	    }

	    private void doAccept(SelectionKey key) throws IOException {
	    	if(stopAccept){
	    		return;
	    	}
	        SocketChannel sc = serverSocketChannel.accept();
	        sc.configureBlocking(false);
	        SelectionKey clientKey = sc.register(selector,SelectionKey.OP_READ);
	        clientKey.attach(new Context(clientKey));
	    }



	    private static void silentlyClose(SelectableChannel sc) {
	        if (sc==null)
	            return;
	        try {
	            sc.close();
	        } catch (IOException e) {
	            // silently ignore
	        }
	    }

		private static void usage() {
	        System.out.println("Server <listeningPort>");
	    }
		
		public static void main(String[] args) throws IOException {
			Server server = new Server(7777);
			
		}

}
