package io.zbus.mq.net;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.api.Message;
import io.zbus.net.IoAdaptor;
import io.zbus.net.Session;
import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory;

public class MessageAdaptor implements IoAdaptor{    
	private static final Logger log = LoggerFactory.getLogger(MessageAdaptor.class);
	 
	protected MessageHandler filterHandler;   
	protected Map<String, MessageHandler> cmdHandlerMap = new ConcurrentHashMap<String, MessageHandler>();
	protected Map<String, MessageProcessor> urlHandlerMap = new ConcurrentHashMap<String, MessageProcessor>();
	protected Map<String, Session> sessionTable;
	
	private static final String CMD_HEARTBEAT = "heartbeat";
	
	public MessageAdaptor(){ 
		this(new ConcurrentHashMap<String, Session>());
	}
	
	public MessageAdaptor(Map<String, Session> sessionTable){
		this.sessionTable = sessionTable;
		this.cmd(CMD_HEARTBEAT, new MessageHandler() { 
			public void handle(Message msg, Session sess) throws IOException { 
				//ignore
			}
		});
	}
	 
	public void cmd(String command, MessageHandler handler){
    	this.cmdHandlerMap.put(command, handler);
    }
	
	public void url(String path, final MessageProcessor processor){
		if(!path.startsWith("/")){
			path = "/"+path;
		}
		
		this.urlHandlerMap.put(path, processor);
		String cmd = path;
		if(cmd.startsWith("/")){
			cmd = cmd.substring(1);
		}
		cmd(cmd, new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException {
				final String msgId = msg.getId();
				Message res = processor.process(msg);
				if(res != null){
					res.setId(msgId);
					sess.write(res);
				}
			}
		});
	}
	 
    public void registerFilterHandler(MessageHandler filterHandler) {
		this.filterHandler = filterHandler;
	}  
    
    @Override
    public void sessionData(Object obj, Session sess) throws IOException {  
    	Message msg = (Message)obj;  
    	final String msgId = msg.getId();
    	
    	if(this.filterHandler != null){
    		this.filterHandler.handle(msg, sess);
    	}
    	
    	String cmd = msg.getCmd();
    	if(cmd != null){ //cmd
    		MessageHandler handler = cmdHandlerMap.get(cmd);
        	if(handler != null){
        		handler.handle(msg, sess);
        		return;
        	}
    	}
    	
    	String url = msg.getUrl();
    	if(url == null){ 
    		Message res = new Message();
    		res.setId(msgId); 
        	res.setStatus(400);
        	res.setBody("Bad Format(400): Missing Command and RequestPath"); 
        	sess.write(res);
    		return;
    	}
    	
    	MessageProcessor urlHandler = urlHandlerMap.get(url);
    	if(urlHandler != null){
    		Message res = null; 
    		try{
    			res = urlHandler.process(msg); 
	    		if(res != null){
	    			res.setId(msgId);
	    			if(res.getStatus() == null){
	    				res.setStatus(200);// default to 200
	    			}
	    			sess.write(res);
	    		}
    		} catch (Exception e) { 
    			res = new Message();
    			res.setStatus(500);
    			res.setBody("Internal Error(500): " + e);
    			sess.write(res);
			}
    
    		return;
    	} 
    	
    	Message res = new Message();
    	res.setId(msgId); 
    	res.setStatus(404);
    	String text = String.format("Not Found(404): %s", url);
    	res.setBody(text); 
    	sess.write(res); 
    } 
     
	@Override
	public void sessionActive(Session sess) throws IOException {
		log.info("Session Created: " + sess);
		sessionTable.put(sess.id(), sess);
	}

	@Override
	public void sessionInactive(Session sess) throws IOException {
		log.info("Session Destroyed: " + sess);
		cleanSession(sess);
	}
 
	@Override
	public void sessionError(Throwable e, Session sess) throws Exception {
		log.info("Session Error: " + sess);
		cleanSession(sess);
	} 

	@Override
	public void sessionIdle(Session sess) throws IOException { 
		log.info("Session Idle Remove: " + sess);
		cleanSession(sess);
	}
	
	protected void cleanSession(Session sess) throws IOException {
		try{
			sess.close();
		} finally {
			sessionTable.remove(sess.id());
		} 
	}

	@Override
	public void sessionRegistered(Session sess) throws IOException {
		
	}

	@Override
	public void sessionUnregistered(Session sess) throws IOException {
		
	} 
	
	public static interface MessageHandler {
		void handle(Message msg, Session session) throws IOException;   
	}
	
	public static interface MessageProcessor {
		Message process(Message request);
	}
}

