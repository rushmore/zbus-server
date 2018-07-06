package io.zbus.rpc.biz;

public class UserException extends Exception { 
	private static final long serialVersionUID = -6135508809283998375L; 
	
	public UserException() { 
	} 

	public UserException(String message, Throwable cause) {
		super(message, cause); 
	}

	public UserException(String message) {
		super(message); 
	}

	public UserException(Throwable cause) {
		super(cause); 
	}
}
