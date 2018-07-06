package io.zbus.transport;

import java.io.Closeable;

/**
 * 
 * Session represents a connection between server and client
 * It's main function is to write message data to peer side of the session.
 * 
 * Typical session types: TCP, InProc, IPC
 * 
 * @author leiming.hong Jun 27, 2018
 *
 */
public interface Session extends Closeable { 
	/** 
	 * @return Id of the session
	 */
	String id();  
	/** 
	 * @return remote address of the session
	 */
	String remoteAddress();
	/**
	 * @return local address of the session
	 */
	String localAddress();
	
	/**
	 * Write data into peer side of the session. 
	 * On the other hand, reading message from session is event-driven in <code>IoAdaptor</code>
	 * @param msg message object before codec
	 */
	void write(Object msg); 
	
	/** 
	 * @return true if session is active: connected
	 */
	boolean active(); 
	
	/**
	 * Get attached attribute value
	 * @param key attribute key
	 * @return attached attribute value
	 */
	<V> V attr(String key);
	
	/**
	 * Attach attribute key-value 
	 * @param key attribute key string
	 * @param value any type of value attached to the session
	 */
	<V> void attr(String key, V value);
}
