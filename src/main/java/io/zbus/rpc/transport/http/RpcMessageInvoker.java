package io.zbus.rpc.transport.http;

import java.io.IOException;

import io.zbus.rpc.MessageInvoker;
import io.zbus.transport.ResultCallback;
import io.zbus.transport.http.Message;
import io.zbus.transport.http.MessageClient;
import io.zbus.transport.http.MessageClientPool; 

public class RpcMessageInvoker implements MessageInvoker { 
	private MessageClientPool clientPool;
	private String token;
	public RpcMessageInvoker(MessageClientPool clientPool){  
		this.clientPool = clientPool;
	}
	
	@Override
	public Message invokeSync(Message req, int timeout) throws IOException, InterruptedException {  
		MessageClient client = null;
		try{ 
			client = clientPool.borrowClient();
			fillCommonHeaders(req);
			return client.invokeSync(req, timeout);
		} finally {
			clientPool.returnClient(client);
		} 
	}

	@Override
	public void invokeAsync(Message req, final ResultCallback<Message> callback) throws IOException {  
		MessageClient client = null;
		try{ 
			client = clientPool.borrowClient();
			fillCommonHeaders(req);
			client.invokeAsync(req, callback);
		} finally {
			clientPool.returnClient(client);
		} 
	}
	
	private void fillCommonHeaders(Message msg) {
		if(this.token != null) {
			if(msg.getHeader("token") == null) {
				msg.setHeader("token", this.token);
			}
		}
	}

	public void setToken(String token) {
		this.token = token;
	}   
}
