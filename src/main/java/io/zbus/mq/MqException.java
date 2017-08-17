package io.zbus.mq;

public class MqException extends RuntimeException {  
	private static final long serialVersionUID = 6006204188240205218L;
    public MqException(String message){
    	super(message);
    }
	public MqException() {
		super(); 
	}
	public MqException(String message, Throwable cause) {
		super(message, cause); 
	}
	public MqException(Throwable cause) {
		super(cause); 
	}   
}
