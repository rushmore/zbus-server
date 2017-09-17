package io.zbus.transport;
 
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.transport.Sync.Ticket;


public abstract class AbstractClient<REQ extends Id, RES extends Id> extends AttributeMap implements Client<REQ, RES> {
	private static final Logger log = LoggerFactory.getLogger(AbstractClient.class); 
	 
	protected Session session;  
	protected int invokeTimeout = 3000;
	protected int connectTimeout = 3000;  
	
	protected CountDownLatch activeLatch = new CountDownLatch(1);    
	protected final Sync<REQ, RES> sync = new Sync<REQ, RES>();    
	
	protected volatile MessageHandler<RES> msgHandler; 
	protected volatile ErrorHandler errorHandler;
	protected volatile ConnectedHandler connectedHandler;
	protected volatile DisconnectedHandler disconnectedHandler;  
	  
	 
	public AbstractClient(){  
		
		onConnected(new ConnectedHandler() { 
			@Override
			public void onConnected() throws IOException {
				String msg = String.format("Connection(%s) OK", serverAddress());
				log.info(msg);
			}
		});
		
		onDisconnected(new DisconnectedHandler() { 
			@Override
			public void onDisconnected() throws IOException {
				log.warn("Disconnected from(%s)", serverAddress());
				ensureConnectedAsync();//automatically reconnect by default
			}
		}); 
	}  
	  
	protected abstract String serverAddress();
	 
	
	protected synchronized void cleanSession() throws IOException{
		if(session != null){
			session.close();
			session = null;
			activeLatch = new CountDownLatch(1);
		} 
	}
	 
	
	public boolean hasConnected() {
		return session != null && session.active();
	}
	
	private Thread asyncConnectThread; 
	public void ensureConnectedAsync(){
		if(hasConnected()) return;
		if(asyncConnectThread != null) return;
		
		asyncConnectThread = new Thread(new Runnable() { 
			@Override
			public void run() {
				try {
					while(!hasConnected()){
						try{
							connectSync(connectTimeout); 
							if(!hasConnected()){
								Thread.sleep(connectTimeout);
							}
						} catch (IOException e) {    
							String msg = String.format("Trying again(%s) in %.1f seconds", serverAddress(), connectTimeout/1000.0); 
							log.warn(msg); 
							Thread.sleep(connectTimeout);
						} catch (InterruptedException e) {
							throw e;
						} catch (Exception e) {
							log.error(e.getMessage(), e);
							break;
						}  
					} 
					asyncConnectThread = null;
				} catch (InterruptedException e) {
					//ignore
				}  
			}
		}); 
		asyncConnectThread.start(); 
	}
	
	
	public void sendMessage(REQ req) throws IOException, InterruptedException{
		if(!hasConnected()){
			connectSync(connectTimeout);  
			if(!hasConnected()){
				String msg = String.format("Connection(%s) failed", serverAddress()); 
				throw new IOException(msg);
			}
		}  
		session.write(req);  
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
	}
	
	public void onMessage(MessageHandler<RES> msgHandler){
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
	public void sessionCreated(Session session) throws IOException { 
		this.session = session;
		activeLatch.countDown();
		if(connectedHandler != null){
			connectedHandler.onConnected();
		}
	}

	public void sessionToDestroy(Session session) throws IOException {
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
	public void onError(Throwable e, Session sess) throws IOException { 
		if(errorHandler != null){
			errorHandler.onError(e, session);
		} else {
			log.error(e.getMessage(), e);
		}
	} 
	
	@Override
	public void onIdle(Session sess) throws IOException { 
		
	}
	 
	public void invokeAsync(REQ req, ResultCallback<RES> callback) throws IOException { 
		Ticket<REQ, RES> ticket = null;
		if(callback != null){
			ticket = sync.createTicket(req, invokeTimeout, callback);
		} else {  
			if(req.getId() == null){ //if message id missing, set it
				req.setId(Sync.nextId()); 
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
		return this.invokeSync(req, this.invokeTimeout);
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
	public void onMessage(Object msg, Session sess) throws IOException {
		@SuppressWarnings("unchecked")
		RES res = (RES)msg;   
    	Ticket<REQ, RES> ticket = sync.removeTicket(res);
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
	
	public int getInvokeTimeout() {
		return invokeTimeout;
	}

	public void setInvokeTimeout(int invokeTimeout) {
		this.invokeTimeout = invokeTimeout;
	}   
}
