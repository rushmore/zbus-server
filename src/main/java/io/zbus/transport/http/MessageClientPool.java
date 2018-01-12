package io.zbus.transport.http;
 
import java.io.Closeable;
import java.io.IOException;

import io.zbus.mq.MqException;
import io.zbus.transport.ClientFactory;
import io.zbus.transport.EventLoop;
import io.zbus.transport.Pool;
import io.zbus.transport.ServerAddress;

public class MessageClientPool implements Closeable {
	private Pool<MessageClient> pool; 
	private MessageClientFactory factory; 
	 
	private ServerAddress serverAddress;
	private int clientPoolSize;
	private EventLoop loop;    
	
	public MessageClientPool(String address, int clientPoolSize, EventLoop loop){    
		this.serverAddress = new ServerAddress(address, this.loop.isSslEnabled());
		buildPool(this.serverAddress, clientPoolSize, loop);
	}
	
	public MessageClientPool(String serverAddress, int clientPoolSize){
		this(serverAddress, clientPoolSize, null);
	}
	
	public MessageClientPool(String serverAddress){
		this(serverAddress, 64);
	}  
	
	public MessageClientPool(ServerAddress serverAddress, int clientPoolSize, EventLoop loop){    
		buildPool(serverAddress, clientPoolSize, loop); 
	}
	
	private void buildPool(ServerAddress serverAddress, int clientPoolSize, EventLoop loop){    
		this.serverAddress = serverAddress;
		this.clientPoolSize = clientPoolSize; 
		if(loop != null){
			this.loop = loop.duplicate(); 
		} else {
			this.loop = new EventLoop(); 
		}
		
		this.factory = new MessageClientFactory(serverAddress, this.loop);
		this.pool = new Pool<MessageClient>(factory, this.clientPoolSize);  
	}

	public MessageClient borrowClient(){
		try {
			MessageClient client = this.pool.borrowObject();  
			return client;
		} catch (Exception e) {
			throw new MqException(e.getMessage(), e);
		} 
	}
	
	public void returnClient(MessageClient... client){
		if(client == null || client.length == 0) return;
		for(MessageClient c : client){
			this.pool.returnObject(c);
		}
	}
	
	public MessageClient createClient(){
		return factory.createObject();
	}
	
	public ServerAddress serverAddress(){
		return serverAddress;
	}  
	
	@Override
	public void close() throws IOException {
		if(this.pool != null){
			this.pool.close();  
			loop.close(); 
			this.pool = null;
		} 
	}
	
	private static class MessageClientFactory extends ClientFactory<Message, Message, MessageClient>{ 
		private EventLoop loop;  
		public MessageClientFactory(ServerAddress serverAddress, EventLoop loop){ 
			super(serverAddress);
			this.loop = loop;
		} 
		
		public MessageClient createObject() { 
			return new MessageClient(serverAddress, loop);
		}  
	} 
}
