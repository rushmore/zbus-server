package io.zbus.rpc;


public interface Protocol {   
	//Compatible to HTTP
	public static final String STATUS    = "status";  // Response message status
	public static final String URL       = "url";     // Message URL(compatible to HTTP)  
	public static final String BODY      = "body";    // Message body  
	public static final String ID        = "id";      // Message ID
	
	public static final String API_KEY   = "apiKey";
	public static final String SIGNATURE = "signature"; 
}
