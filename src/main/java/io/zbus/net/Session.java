package io.zbus.net;

import java.io.Closeable;

import io.netty.channel.ChannelFuture;

public interface Session extends Closeable {
	
	String id(); 
	
	String getRemoteAddress();
	
	String getLocalAddress();
	
	ChannelFuture write(Object msg);
	
	ChannelFuture writeAndFlush(Object msg);
	
	void flush();
	
	boolean isActive(); 
	
	<V> V attr(String key);
	
	<V> void attr(String key, V value);
}
