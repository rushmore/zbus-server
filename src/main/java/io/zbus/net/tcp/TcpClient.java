package io.zbus.net.tcp;
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.zbus.net.Client;
import io.zbus.net.CodecInitializer;
import io.zbus.net.IoDriver;
import io.zbus.net.Session;
import io.zbus.net.Sync;
import io.zbus.net.Sync.Id;
import io.zbus.net.Sync.ResultCallback;
import io.zbus.net.Sync.Ticket;
import io.zbus.util.log.Logger;
import io.zbus.util.log.LoggerFactory;


public class TcpClient<REQ extends Id, RES extends Id> implements Client<REQ, RES> {
	private static final Logger log = LoggerFactory.getLogger(TcpClient.class); 
	
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
	
	private ConcurrentMap<String, Object> attributes = null;
	
	protected final Sync<REQ, RES> sync = new Sync<REQ, RES>();  
	
	protected volatile ScheduledExecutorService heartbeator = null;
	
	protected volatile MsgHandler<RES> msgHandler; 
	protected volatile ErrorHandler errorHandler;
	protected volatile ConnectedHandler connectedHandler;
	protected volatile DisconnectedHandler disconnectedHandler;  
	
	public TcpClient(String address, IoDriver driver){  
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
				ensureConnectedAsync();//automatically reconnect by default
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
	
	private synchronized void cleanSession() throws IOException{
		if(session != null){
			session.close();
			session = null;
			activeLatch = new CountDownLatch(1);
		} 
	}
	 
	public void codec(CodecInitializer codecInitializer) {
		this.codecInitializer = codecInitializer;
	} 
	
	public synchronized void startHeartbeat(int heartbeatInSeconds){
		if(heartbeator == null){
			heartbeator = Executors.newSingleThreadScheduledExecutor();
			this.heartbeator.scheduleAtFixedRate(new Runnable() {
				public void run() {
					try {
						heartbeat();
					} catch (Exception e) {
						log.warn(e.getMessage(), e);
					}
				}
			}, heartbeatInSeconds, heartbeatInSeconds, TimeUnit.SECONDS);
		}
	}
	
	@Override
	public void stopHeartbeat() { 
		if(heartbeator != null){
			heartbeator.shutdown();
		}
	}
	
	@Override
	public void heartbeat() {
		
	}
	
	
	public boolean hasConnected() {
		return session != null && session.isActive();
	}
	
	private Thread asyncConnectThread; 
	public void ensureConnectedAsync(){
		if(hasConnected()) return;
		if(asyncConnectThread != null) return;
		
		asyncConnectThread = new Thread(new Runnable() { 
			@Override
			public void run() {
				try {
					ensureConnected();
					asyncConnectThread = null;
				} catch (InterruptedException e) {
					//ignore
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			}
		});
		asyncConnectThread.setName("ClientConnectionAync");
		asyncConnectThread.start(); 
	}
	
	
	public synchronized void connectAsync(){  
		init(); 
		
		channelFuture = bootstrap.connect(host, port);
	}   

	@Override
	public void connectSync(long timeout) throws IOException, InterruptedException {
		if(hasConnected()) return; 
		
		synchronized (this) {
			if(!hasConnected()){ 
	    		connectAsync();
				activeLatch.await(readTimeout,TimeUnit.MILLISECONDS);
				
				if(hasConnected()){ 
					return;
				}  
				String msg = String.format("Connection(%s:%d) timeout", host, port); 
				log.warn(msg);
				cleanSession(); 
			}
		} 
	}
	
	public void ensureConnected() throws IOException, InterruptedException{
		while(!hasConnected()){
			connectSync(connectTimeout); 
			if(hasConnected()) break;
			
			String msg = String.format("Trying again in %.1f seconds", connectTimeout/1000.0); 
			log.warn(msg); 
			Thread.sleep(connectTimeout);
		} 
	} 
	
	public void sendMessage(REQ req) throws IOException, InterruptedException{
		if(!hasConnected()){
			connectSync(10000);  
			if(!hasConnected()){
				String msg = String.format("Connection(%s:%d) timeout", host, port); 
				throw new IOException(msg);
			}
		}  
		session.writeAndFlush(req); 
    	
    } 
	
	 
	@Override
	public void close() throws IOException {
		onConnected(null);
		onDisconnected(null);
		
		if(asyncConnectThread != null){
			asyncConnectThread.interrupt();
			asyncConnectThread = null;
		}
		
		if(session != null){
			session.close();
			session = null;
		}  
		if(heartbeator != null){
			heartbeator.shutdownNow();
			heartbeator = null;
		} 
	}
	
	 
	@SuppressWarnings("unchecked")
	public <V> V attr(String key) {
		if (this.attributes == null) {
			return null;
		}

		return (V) this.attributes.get(key);
	}

	public <V> void attr(String key, V value) {
		if(value == null){
			if(this.attributes != null){
				this.attributes.remove(key);
			}
			return;
		}
		if (this.attributes == null) {
			synchronized (this) {
				if (this.attributes == null) {
					this.attributes = new ConcurrentHashMap<String, Object>();
				}
			}
		}
		this.attributes.put(key, value);
	}
	
	public void onMessage(MsgHandler<RES> msgHandler){
    	this.msgHandler = msgHandler;
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
		sync.clearTicket();
		
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
	 
	public void invokeAsync(REQ req, ResultCallback<RES> callback) throws IOException { 
		Ticket<REQ, RES> ticket = null;
		if(callback != null){
			ticket = sync.createTicket(req, readTimeout, callback);
		} else {
			if(req.getId() == null){
				req.setId(Ticket.nextId());
			}
		} 
		try{
			sendMessage(req); 
		} catch(IOException e) {
			if(ticket != null){
				sync.removeTicket(ticket.getId());
			}
			throw e;
		} catch (InterruptedException e) {
			log.warn(e.getMessage(), e);
		}  
	} 
	
	public RES invokeSync(REQ req) throws IOException, InterruptedException {
		return this.invokeSync(req, this.readTimeout);
	}
	 
	public RES invokeSync(REQ req, int timeout) throws IOException, InterruptedException {
		Ticket<REQ, RES> ticket = null;
		try { 
			ticket = sync.createTicket(req, timeout);
			sendMessage(req);   
			if (!ticket.await(timeout, TimeUnit.MILLISECONDS)) {
				return null;
			}
			return ticket.response();
		} finally {
			if (ticket != null) {
				sync.removeTicket(ticket.getId());
			}
		}
	} 
	
	@Override
	public void onSessionMessage(Object msg, Session sess) throws IOException {
		@SuppressWarnings("unchecked")
		RES res = (RES)msg;  
    	Ticket<REQ, RES> ticket = sync.removeTicket(res.getId());
    	if(ticket != null){
    		ticket.notifyResponse(res); 
    		return;
    	}   
    	
    	if(msgHandler != null){
    		msgHandler.handle(res, sess);
    		return;
    	} 
    	
    	log.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!Drop,%s", res);
	}  
	
	@Override
	public String toString() { 
		return String.format("(connected=%s, remote=%s:%d)", hasConnected(), host, port);
	}
	 
}
