package io.zbus.transport;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;

public abstract class ServerAdaptor implements IoAdaptor{    
	private static final Logger log = LoggerFactory.getLogger(ServerAdaptor.class); 
	protected Map<String, Session> sessionTable;
	
	public ServerAdaptor(){ 
		this(new ConcurrentHashMap<String, Session>());
	}
	
	public ServerAdaptor(Map<String, Session> sessionTable){
		if(sessionTable == null){
			sessionTable = new ConcurrentHashMap<String, Session>();
		}
		this.sessionTable = sessionTable; 
	}  
     
	@Override
	public void sessionCreated(Session sess) throws IOException {
		log.info("Created: " + sess);
		sessionTable.put(sess.id(), sess);
	}

	@Override
	public void sessionToDestroy(Session sess) throws IOException {
		log.info("Destroyed: " + sess);
		cleanSession(sess);
	}
 
	@Override
	public void onError(Throwable e, Session sess) throws Exception { 
		log.info("Error: " + sess, e);
		cleanSession(sess);
	} 

	@Override
	public void onIdle(Session sess) throws IOException { 
		log.info("Idled: " + sess);
		cleanSession(sess);
	}
	
	protected void cleanSession(Session sess) throws IOException {
		try{
			sess.close();
		} finally {
			sessionTable.remove(sess.id());
		} 
	}
}

