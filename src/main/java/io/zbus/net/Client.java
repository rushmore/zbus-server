package io.zbus.net;
 
import java.io.Closeable;
import java.io.IOException;

/**
 * 
 * Caution: when caller use asynchronous way to send or invoke REQ, the REQ message should not be reused,
 * since the underlying do NOT copy message, the message may still in queue to deliver, modification on the
 * ongoing message may cause undefined behavior.
 * 
 * @author Rushmore
 *
 * @param <REQ>
 * @param <RES>
 */
public interface Client<REQ, RES> extends IoAdaptor, Closeable {  
	void codec(CodecInitializer codecInitializer); 
	boolean hasConnected(); 
	Future<Void> connect();  
	
	Future<Void> send(REQ req); 

	Future<RES> invoke(REQ req); 
	
	void onMessage(MsgHandler<RES> dataHandler);
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
	
	public static interface MsgHandler<T> { 
		void onMessage(T msg, Session session) throws IOException;   
	}  
}
