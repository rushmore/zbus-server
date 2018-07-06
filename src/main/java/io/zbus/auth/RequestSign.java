package io.zbus.auth;

import io.zbus.transport.Message;

/**
 * Digital signature for JSON typed request
 * 
 * @author leiming.hong
 *
 */
public interface RequestSign {    
	public static final String APIKEY = "apiKey";
	public static final String SIGNATURE = "signature";  
	
	/**
	 * Calculate digital signature value of JSON request based on apiKey and secret key
	 * The request object remains unchanged
	 * 
	 * @param request JSON typed request object
	 * @param apiKey public key for signature
	 * @param secret private key for signature
	 * @return signed string value of request
	 */
	String calcSignature(Message request, String apiKey, String secret); 
	
	/**
	 * Calculate digital signature value of JSON request based on apiKey and secret key, and
	 * set apiKey and signature key-value to request object, request object gets updated! 
	 * 
	 * @param request JSON typed request object
	 * @param apiKey public key for signature
	 * @param secret private key for signature
	 */
	void sign(Message request, String apiKey, String secret);
}
