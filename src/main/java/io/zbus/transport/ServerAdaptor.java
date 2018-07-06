package io.zbus.transport;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base server side <code>IoAdaptor</code>, handles all IO events except for onMessage.
 * 
 * Subclass ServerAdaptor can take advantage of the sessionTable management in this base class
 * 
 * @author leiming.hong Jun 27, 2018
 *
 */
public abstract class ServerAdaptor implements IoAdaptor{    
	private static final Logger logger = LoggerFactory.getLogger(ServerAdaptor.class); 
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
		logger.info("Created: " + sess);
		sessionTable.put(sess.id(), sess);
	}

	@Override
	public void sessionToDestroy(Session sess) throws IOException {
		logger.info("Destroyed: " + sess);
		cleanSession(sess);
	}
 
	@Override
	public void onError(Throwable e, Session sess) { 
		logger.info("Error: " + sess, e);
		try {
			cleanSession(sess);
		} catch (IOException ex) {
			logger.error(ex.getMessage(), ex);
		}
	} 

	@Override
	public void onIdle(Session sess) throws IOException { 
		logger.info("Idled: " + sess);
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

