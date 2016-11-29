package io.zbus.mq.api;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * Status[null], null usually interpreted as request
 * Header(Key-Value pairs)
 * Body 
 * 
 * Message is designed easy to be serialized as HTTP format.
 * 
 * @author Rushmore
 *
 */
public class Message{
	public static final String Cmd       = "Cmd";
	public static final String Topic     = "Topic";
	public static final String Group     = "Group";
	public static final String AppId     = "AppId";
	public static final String Token     = "Token";
	public static final String Ack       = "Ack";
	public static final String Tag       = "Tag";
	public static final String Id        = "Id";
	public static final String Sender    = "Sender";
	public static final String Receiver  = "Receiver";
	public static final String Window    = "Window";
	public static final String BatchSize = "BatchSize";
	public static final String BatchInTx = "BatchInTx"; 
	 
	private Integer status;
	private Map<String, String> headers = new HashMap<String, String>();
	private byte[] body;
	
	
    public Message cmd(String value){ 
    	return header(Cmd, value); 
    }
    public String cmd(){ 
    	return header(Cmd);
    }
    
    public Message topic(String value){ 
    	return header(Topic, value); 
    }
    public String topic(){ 
    	return header(Topic);
    }    
    
    public String group(){ 
    	return header(Group);
    }    
    public Message group(String value){ 
    	return header(Group, value); 
    }
    
    public String appId(){ 
    	return header(AppId);
    }    
    public Message appId(String value){ 
    	return header(AppId, value); 
    }
    
    public String token(){ 
    	return header(Token);
    }    
    public Message token(String value){ 
    	return header(Token, value); 
    }
    
    public boolean ack(){ 
    	String value = header(Ack);
    	return value == null? false : Boolean.valueOf(value);
    }    
    public Message ack(boolean value){ 
    	return header(Ack, String.valueOf(value)); 
    }
    
    public String id(){ 
    	return header(Id);
    }    
    public Message id(String value){ 
    	return header(Id, value); 
    }
    
    public String sender(){ 
    	return header(Sender);
    }    
    public Message sender(String value){ 
    	return header(Sender, value); 
    }
    
    public String receiver(){ 
    	return header(Receiver);
    }    
    public Message receiver(String value){ 
    	return header(Receiver, value); 
    }
    
    public Integer window(){ 
    	String value = header(Window);
    	return value == null? null : Integer.valueOf(value);
    }    
    public Message window(Integer value){ 
    	return header(Window, String.valueOf(value)); 
    }
    
    public Integer batchSize(){ 
    	String value = header(BatchSize);
    	return value == null? null : Integer.valueOf(value);
    }    
    public Message batchSize(Integer value){ 
    	return header(BatchSize, String.valueOf(value)); 
    }
    
    public boolean batchInTx(){ 
    	String value = header(BatchInTx);
    	return value == null? false : Boolean.valueOf(value);
    }    
    public Message batchInTx(boolean value){ 
    	return header(BatchInTx, String.valueOf(value)); 
    }
    
    
    public Integer status() {
		return status;
	}
	public Message status(Integer value) {
		this.status = value;
		return this;
	}
    
    public String header(String key){
		if(!headers.containsKey(key)) return null;
		return headers.get(key);
	}  
    
	public Message header(String key, String value){
		headers.put(key, value);
		return this;
	}  
	
	public Map<String, String> headers() {
		return headers;
	}
	
	public byte[] body() {
		return body;
	}
	
	public Message body(byte[] value) {
		this.body = value;
		return this;
	}  
	
	public Message body(String value) {
		return body(value.getBytes());
	} 
	
	public Message body(String value, Charset charset) {
		return body(value.getBytes(charset));
	}  
}
