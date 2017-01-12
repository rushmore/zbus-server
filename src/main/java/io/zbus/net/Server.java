package io.zbus.net;

import java.io.Closeable;

public interface Server extends Closeable{ 
	void codec(CodecInitializer codecInitializer);
	EventDriver getEventDriver();
	void start(int port, IoAdaptor ioAdaptor) throws Exception;
	void start(String host, int port, IoAdaptor ioAdaptor) throws Exception; 
	void join() throws InterruptedException;
}
