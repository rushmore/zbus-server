package io.zbus.net;
 
import java.io.Closeable;
import java.io.IOException;
  
public interface Client<REQ, RES> extends Invoker<REQ, RES>, IoAdaptor, Closeable { 
	
	void codec(CodecInitializer codecInitializer);
	
	void startHeartbeat(int heartbeatInSeconds);
	void stopHeartbeat();
	void heartbeat();
	
	boolean hasConnected();
	void connectAsync() throws IOException; 
	void connectSync(long timeout) throws IOException, InterruptedException;
	void ensureConnectedAsync();
	void ensureConnected() throws IOException, InterruptedException;
	
	void sendMessage(REQ req) throws IOException, InterruptedException;; 
	void onMessage(MsgHandler<RES> msgHandler);
	void onError(ErrorHandler errorHandler);
    void onConnected(ConnectedHandler connectedHandler);
    void onDisconnected(DisconnectedHandler disconnectedHandler);
    
    <V> V attr(String key);
	<V> void attr(String key, V value);
    
    
	public static interface ConnectedHandler { 
		void onConnected() throws IOException;   
	}
	
	public static interface DisconnectedHandler { 
		void onDisconnected() throws IOException;   
	}
	
	public static interface ErrorHandler { 
		void onError(Throwable e, Session session) throws IOException;   
	}
	
	public static interface MsgHandler<T> { 
		void handle(T msg, Session session) throws IOException;   
	}  
}
