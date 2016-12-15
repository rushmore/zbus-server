package io.zbus.net.tcp;
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.zbus.net.Client;
import io.zbus.net.CodecInitializer;
import io.zbus.net.Future;
import io.zbus.net.FutureListener;
import io.zbus.net.IoDriver;
import io.zbus.net.Session;
import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory;


public class TcpClient<REQ, RES> extends AttributeMap implements Client<REQ, RES> {
	private static final Logger log = LoggerFactory.getLogger(TcpClient.class); 
	
	private Bootstrap bootstrap;
	protected final EventLoopGroup eventGroup;  
	protected SslContext sslCtx;
	protected Future<Void> connectFuture; 
	protected CodecInitializer codecInitializer; 
	
	protected Session session; 
	protected final String host;
	protected final int port;  
	protected int reconnectTimeMs = 3000;
	
	protected volatile DataHandler<RES> dataHandler; 
	protected volatile ErrorHandler errorHandler;
	protected volatile ConnectedHandler connectedHandler;
	protected volatile DisconnectedHandler disconnectedHandler;  
	
	public TcpClient(String address, IoDriver driver){  
		eventGroup = driver.getGroup();
		sslCtx = driver.getSslContext();
		
		String[] bb = address.split(":");
		if(bb.length > 2) {
			throw new IllegalArgumentException("Address invalid: "+ address);
		}
		host = bb[0].trim();
		if(bb.length > 1){
			port = Integer.valueOf(bb[1]);
		} else {
			port = 80;
		}  
		
		onConnected(new ConnectedHandler() { 
			@Override
			public void onConnected() throws IOException { 
				log.info("Connection(%s:%d) OK", host, port);
			}
		});
		
		onDisconnected(new DisconnectedHandler() { 
			@Override
			public void onDisconnected() throws IOException {
				connectFuture = null;
				log.warn("Disconnected from(%s:%d)", host, port);
				log.info("Trying to reconnect in %.1f seconds", reconnectTimeMs/1000.0);
				try {
					Thread.sleep(reconnectTimeMs);
				} catch (InterruptedException e) { 
					return;
				} 
				connect();
			}
		});
	} 
	
	public String getConnectedServerAddress(){
		return host+":"+port;
	}

	private void init(){
		if(bootstrap != null) return;
		
		bootstrap = new Bootstrap();
		bootstrap.group(this.eventGroup) 
		 .channel(NioSocketChannel.class)  
		 .handler(new ChannelInitializer<SocketChannel>() { 
			NettyToIoAdaptor nettyToIoAdaptor = new NettyToIoAdaptor(TcpClient.this);
			@Override
			protected void initChannel(SocketChannel ch) throws Exception { 
				if(codecInitializer == null){
					log.warn("Missing codecInitializer"); 
				} 
				ChannelPipeline p = ch.pipeline();
				if(sslCtx != null){
					p.addLast(sslCtx.newHandler(ch.alloc()));
				}
				if(codecInitializer != null){
					List<ChannelHandler> handlers = new ArrayList<ChannelHandler>();
					codecInitializer.initPipeline(handlers);
					for(ChannelHandler handler : handlers){ 
						p.addLast((ChannelHandler)handler);
					}
				}
				p.addLast(nettyToIoAdaptor);
			}
		});  
	}   
	 
	public void codec(CodecInitializer codecInitializer) {
		this.codecInitializer = codecInitializer;
	} 
	
	public synchronized void startHeartbeat(int heartbeatInSeconds){ 
	}
	
	@Override
	public void stopHeartbeat() {  
	}
	
	@Override
	public void heartbeat() { 
	}
	
	
	public boolean hasConnected() {
		return session != null && session.isActive();
	}  
	
	
	public synchronized Future<Void> connect(){
		if(this.connectFuture != null) return this.connectFuture; 
		init(); 
		
		this.connectFuture = new DefaultFuture<Void>(bootstrap.connect(host, port));
		this.connectFuture.addListener(new FutureListener<Void>() { 
			@Override
			public void operationComplete(Future<Void> future) throws Exception { 
				if(!future.isSuccess()){
					Throwable cause = future.cause();
					log.error(cause.getMessage(), cause);
					log.info("Trying to reconnect in %.1f seconds", reconnectTimeMs/1000.0);
					Thread.sleep(reconnectTimeMs);
					connectFuture = null;
					connect();
				}
			}
		});
		return this.connectFuture;
	} 
	
	
	public Future<Void> send(final REQ req){ 
		if(!hasConnected()){
			connect(); 
			return connectFuture.addListener(new FutureListener<Void>() {
				@Override
				public void operationComplete(Future<Void> future) throws Exception {
					if(future.isSuccess()){
						if(session == null){
							throw new IOException("Session not created");
						}
						session.writeAndFlush(req); 
					} else {  
						throw new IOException(future.cause().getMessage(), future.cause());
					}
				}
			}); 
		}
		
		return session.writeAndFlush(req);  
    } 
	
	 
	@Override
	public void close() throws IOException {
		onConnected(null);
		onDisconnected(null); 
		
		if(session != null){
			session.close();
			session = null;
		}   
	} 
	
	public void onData(DataHandler<RES> msgHandler){
    	this.dataHandler = msgHandler;
    }
    
    public void onError(ErrorHandler errorHandler){
    	this.errorHandler = errorHandler;
    } 
    
    public void onConnected(ConnectedHandler connectedHandler){
    	this.connectedHandler = connectedHandler;
    } 
    
    public void onDisconnected(DisconnectedHandler disconnectedHandler){
    	this.disconnectedHandler = disconnectedHandler;
    } 
    
	@Override
	public String toString() { 
		return String.format("(Connected=%s, Remote=%s:%d)", hasConnected(), host, port);
	} 

	@Override
	public void sessionRegistered(Session sess) throws IOException { 
		this.session = sess; 
	}
	
	@Override
	public void sessionActive(Session sess) throws IOException { 
		if(connectedHandler != null){
			connectedHandler.onConnected();
		}
	}

	public void sessionInactive(Session sess) throws IOException {
		if(this.session != null){
			this.session.close(); 
			this.session = null;
		}  
		
		if(disconnectedHandler != null){
			disconnectedHandler.onDisconnected();
		}   
	} 

	@Override
	public void sessionError(Throwable e, Session sess) throws IOException {  
		if(errorHandler != null){
			errorHandler.onError(e, session);
		} else {
			log.error(e.getMessage(), e);
		}
	} 
	
	@Override
	public void sessionIdle(Session sess) throws IOException { 
		log.info(sess + " Idle");
	}
	   
	@Override
	public void sessionUnregistered(Session sess) throws IOException {
		this.session = null;
		this.connectFuture = null;
	}
	
	@Override
	public void sessionData(Object msg, Session sess) throws IOException {
		@SuppressWarnings("unchecked")
		RES res = (RES)msg;   
    	if(dataHandler != null){
    		dataHandler.onData(res, sess);
    		return;
    	} 
    	
    	log.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!Drop,%s", res);
	}  
}
