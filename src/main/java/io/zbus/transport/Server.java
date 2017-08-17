package io.zbus.transport;

import java.io.Closeable;

public interface Server extends Closeable{ 
	void codec(CodecInitializer codecInitializer);
	EventLoop getEventLoop();
	IoAdaptor getIoAdatpr(); //default
	ServerAddress getServerAddress();
	void start(int port, IoAdaptor ioAdaptor) throws Exception;
	void start(String host, int port, IoAdaptor ioAdaptor) throws Exception;  
}
