package io.zbus.rpc;

import java.io.IOException;
 
import io.zbus.transport.ResultCallback;
import io.zbus.transport.http.Message;

public interface MessageInvoker {
	
	Message invokeSync(Message req, int timeout) throws IOException, InterruptedException;
	 
	void invokeAsync(Message req, ResultCallback<Message> callback) throws IOException;
}
