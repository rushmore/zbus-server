package io.zbus.transport;
 
import java.io.Closeable;
import java.io.IOException;
  
/**
 * A TCP client, capable of 
 * 1) remote invocation of message: messaging pairing, message identification from request to response
 * 2) configuration of IO events(via IoAdaptor), such as connection creation/destroy/message received
 *    more API friendly way is using XXHandler such as ConnectedHandler
 * 3) heartbeat 
 * 
 * @author rushmore (洪磊明)
 *
 * @param <REQ> Request message type
 * @param <RES> Response message type
 */
public interface Client<REQ extends Id, RES extends Id> extends Invoker<REQ, RES>, IoAdaptor, Closeable { 

	boolean hasConnected();
	void connectAsync() throws IOException; 
	void connectSync(long timeout) throws IOException, InterruptedException;
	void ensureConnectedAsync(); 
	
	void sendMessage(REQ req) throws IOException, InterruptedException;; 
	void onMessage(MessageHandler<RES> messageHandler);
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
}
