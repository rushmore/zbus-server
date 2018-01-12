package io.zbus.rpc.transport.http;

import java.io.IOException;
import java.util.Map;

import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.rpc.Request;
import io.zbus.rpc.RpcProcessor;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session;
import io.zbus.transport.http.Message;

public class RpcMessageHandler extends ServerAdaptor {
	private static final Logger log = LoggerFactory.getLogger(RpcMessageHandler.class); 
	private final static String TOKEN_KEY = "token";
	protected final RpcProcessor rpcProcessor;
	private String token;
	
	private boolean verbose = false;
	
	public RpcMessageHandler(RpcProcessor rpcProcessor) {
		this.rpcProcessor = rpcProcessor;
	}
	
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
	@Override
	public void onMessage(Object message, Session sess) throws IOException { 
		Message msg = (Message)message; 
		if(verbose) {
			log.info(""+msg);
		}
		
		parseCookieToken(msg);
		handleUrlMessage(msg);
		
		if(token != null) {
			if(!token.equals(msg.getHeader(TOKEN_KEY))) {
				Message res = new Message();
				res.setStatus(403);
				res.setBody("403: Forbbiden");
				sess.write(res);
				return;
			}
		} 
		
		final String msgId  = msg.getId();  
		Message res = rpcProcessor.process(msg); 
		if(res != null){
			res.setId(msgId); 
			if(res.getStatus() == null){
				res.setStatus(200); //default to 200, if not set
			}
			sess.write(res);
		}
	} 
	
	private void parseCookieToken(Message msg){
    	String cookieString = msg.getHeader("cookie");
        if (cookieString != null) {
        	Map<String, String> cookies = StrKit.kvp(cookieString, "[;]");
        	if(cookies.containsKey(TOKEN_KEY)){
        		if(msg.getHeader(TOKEN_KEY) == null){
        			msg.setHeader(TOKEN_KEY, cookies.get(TOKEN_KEY));
        		} 
        	} 
        }
    }
	
	protected void handleUrlMessage(Message msg) {
		String url = msg.getUrl(); 
    	if(url == null || "/".equals(url)){ 
    		return;
    	} 
    	if(msg.getBody() != null) return;
    	
		UrlInfo info = HttpKit.parseUrl(url);    
		String token = info.params.get(TOKEN_KEY); //special for token kv
		if(token != null){
			msg.setHeader(TOKEN_KEY, token);
		}
		
		Request req = new Request();
    	if(info.path.size()>=1){
    		req.setModule(info.path.get(0));
    	}
    	if(info.path.size()>=2){
    		req.setMethod(info.path.get(1));
    	} 
    	
    	if(info.path.size()>2){
    		Object[] params = new Object[info.path.size()-2];
    		for(int i=0;i<params.length;i++){
    			params[i] = info.path.get(2+i);
    		}
    		req.setParams(params); 
    	}   
        msg.setBody(JsonKit.toJSONString(req));  
	}

	public void setToken(String token) {
		this.token = token;
	}
	
}
