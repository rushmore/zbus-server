package io.zbus.auth;
/**
 * apiKey and secretKey mapping provider, main functionality
 * 
 * 1) query secretKey by apiKey
 * 2) check whether apiKey exists
 *  
 * @author leiming.hong
 *
 */
public interface ApiKeyProvider { 
	/**
	 * Query secretKey by apiKey
	 * 
	 * @param apiKey public key
	 * @return private secretKey
	 */
	String secretKey(String apiKey);
	
	/**
	 * Check whether an apiKey exists
	 * 
	 * @param apiKey public key
	 * @return true if exists, false otherwise
	 */
	boolean apiKeyExists(String apiKey);
}
