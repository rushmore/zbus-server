package io.zbus.net;
 
import java.io.Closeable;
import java.io.IOException;

import io.netty.channel.ChannelFuture;
  
public interface Client<REQ, RES> extends IoAdaptor, Closeable {  
	void codec(CodecInitializer codecInitializer); 
	boolean hasConnected(); 
	ChannelFuture connect();  
	
	ChannelFuture send(REQ req); 
	
	void onData(DataHandler<RES> dataHandler);
	void onError(ErrorHandler errorHandler);
    void onConnected(ConnectedHandler connectedHandler);
    void onDisconnected(DisconnectedHandler disconnectedHandler);
    
    <V> V attr(String key);
	<V> void attr(String key, V value);
	
	void startHeartbeat(int heartbeatInSeconds);
	void stopHeartbeat();
	void heartbeat();
    
    
	public static interface ConnectedHandler { 
		void onConnected() throws IOException;   
	}
	
	public static interface DisconnectedHandler { 
		void onDisconnected() throws IOException;   
	}
	
	public static interface ErrorHandler { 
		void onError(Throwable e, Session session) throws IOException;   
	}
	
	public static interface DataHandler<T> { 
		void onData(T data, Session session) throws IOException;   
	}  
}
