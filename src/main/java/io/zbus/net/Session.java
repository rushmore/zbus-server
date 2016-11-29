package io.zbus.net;

import java.io.Closeable;

public interface Session extends Closeable {
	
	String id(); 
	
	String getRemoteAddress();
	
	String getLocalAddress();
	
	void write(Object msg);
	
	void writeAndFlush(Object msg);
	
	void flush();
	
	boolean isActive(); 
	
	<V> V attr(String key);
	
	<V> void attr(String key, V value);
}
