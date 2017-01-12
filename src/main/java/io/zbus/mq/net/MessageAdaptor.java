package io.zbus.mq.net;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.Message;
import io.zbus.net.IoAdaptor;
import io.zbus.net.Session;
import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory;

public class MessageAdaptor implements IoAdaptor{    
	private static final Logger log = LoggerFactory.getLogger(MessageAdaptor.class);
	 
	protected MessageHandler filterHandler;   
	protected Map<String, MessageHandler> cmdHandlerMap = new ConcurrentHashMap<String, MessageHandler>(); 
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
	 
    public void registerFilterHandler(MessageHandler filterHandler) {
		this.filterHandler = filterHandler;
	}  
    
    private void handleUrlMessage(Message msg){ 
    	if(msg.getCmd() != null){
    		return;
    	} 
    	String url = msg.getUrl(); 
    	if(url == null || "/".equals(url)){
    		msg.setCmd("");
    		return;
    	} 
    	int idx = url.indexOf('?');
    	String cmd = "";
    	if(idx >= 0){
    		cmd = url.substring(1, idx); 
    		if(url.charAt(idx-1) == '/'){
    			cmd = url.substring(1, idx-1);
    		} else {
    			cmd = url.substring(1, idx);
    		}
    	} else {
    		cmd = url.substring(1);
    	}  
    	
    	msg.setCmd(cmd);
    	msg.urlToHead(); 
	}
    
    @Override
    public void sessionData(Object obj, Session sess) throws IOException {  
    	Message msg = (Message)obj;  
    	final String msgId = msg.getId();
    	
    	if(this.filterHandler != null){
    		this.filterHandler.handle(msg, sess);
    	}
    	
    	handleUrlMessage(msg); 
    	
    	String cmd = msg.getCmd();
    	if(cmd != null){ //cmd
    		MessageHandler handler = cmdHandlerMap.get(cmd);
        	if(handler != null){
        		handler.handle(msg, sess);
        		return;
        	}
    	}
    	 
    	Message res = new Message();
    	res.setId(msgId); 
    	res.setStatus(404);
    	String text = String.format("Not Found(404): %s", cmd);
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
}

