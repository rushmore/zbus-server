package io.zbus.transport;

import java.io.Closeable;

public interface Session extends Closeable {
	
	String id(); 
	
	String remoteAddress();
	
	String localAddress();
	
	void write(Object msg); 
	
	boolean active(); 
	
	<V> V attr(String key);
	
	<V> void attr(String key, V value);
}
