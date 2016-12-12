package io.zbus.net;
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.zbus.net.tcp.NettyToIoAdaptor;
import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory;


public class DefaultClient<REQ, RES> extends AttributeMap implements SimpleClient<REQ, RES> {
	private static final Logger log = LoggerFactory.getLogger(DefaultClient.class); 
	
	protected Bootstrap bootstrap;
	protected final EventLoopGroup group;  
	protected SslContext sslCtx;
	protected ChannelFuture channelFuture; 
	protected CodecInitializer codecInitializer; 
	
	protected Session session; 
	protected final String host;
	protected final int port; 
	protected int readTimeout = 3000;
	protected int connectTimeout = 3000; 
	protected CountDownLatch activeLatch = new CountDownLatch(1);    
	
	protected volatile DataHandler<RES> dataHandler; 
	protected volatile ErrorHandler errorHandler;
	protected volatile ConnectedHandler connectedHandler;
	protected volatile DisconnectedHandler disconnectedHandler;  
	
	public DefaultClient(String address, IoDriver driver){  
		group = driver.getGroup();
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
				String msg = String.format("Connection(%s:%d) OK", host, port);
				log.info(msg);
			}
		});
		
		onDisconnected(new DisconnectedHandler() { 
			@Override
			public void onDisconnected() throws IOException {
				log.warn("Disconnected from(%s:%d)", host, port); 
				//TODO
			}
		});
	} 
	
	public String getConnectedServerAddress(){
		return host+":"+port;
	}

	private void init(){
		if(bootstrap != null) return;
		
		bootstrap = new Bootstrap();
		bootstrap.group(this.group) 
		 .channel(NioSocketChannel.class)  
		 .handler(new ChannelInitializer<SocketChannel>() { 
			NettyToIoAdaptor nettyToIoAdaptor = new NettyToIoAdaptor(DefaultClient.this);
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
	
	
	public ChannelFuture send(final REQ req) throws IOException, InterruptedException{
		if(!hasConnected()){
			connect();
			return channelFuture.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if(future.isSuccess()){
						session.writeAndFlush(req); 
					} else {  
						throw new IOException(future.cause());
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
	public void onSessionCreated(Session sess) throws IOException { 
		this.session = sess;
		activeLatch.countDown();
		if(connectedHandler != null){
			connectedHandler.onConnected();
		}
	}

	public void onSessionToDestroy(Session sess) throws IOException {
		if(this.session != null){
			this.session.close(); 
			this.session = null;
		}  
		
		if(disconnectedHandler != null){
			disconnectedHandler.onDisconnected();
		}   
	} 

	@Override
	public void onSessionError(Throwable e, Session sess) throws IOException { 
		if(errorHandler != null){
			errorHandler.onError(e, session);
		} else {
			log.error(e.getMessage(), e);
		}
	} 
	
	@Override
	public void onSessionIdle(Session sess) throws IOException { 
		
	}
	   
	
	@Override
	public void onSessionMessage(Object msg, Session sess) throws IOException {
		@SuppressWarnings("unchecked")
		RES res = (RES)msg;   
    	if(dataHandler != null){
    		dataHandler.onData(res, sess);
    		return;
    	} 
    	
    	log.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!Drop,%s", res);
	}  
	
	@Override
	public String toString() { 
		return String.format("(connected=%s, remote=%s:%d)", hasConnected(), host, port);
	}
	 
	
	public synchronized ChannelFuture connect(){
		if(hasConnected()) return this.channelFuture;  
		init(); 
		
		this.channelFuture = bootstrap.connect(host, port);
		return this.channelFuture;
	} 
}
