package io.zbus.mq;
 
import java.io.Closeable;
import java.io.IOException;

import io.zbus.transport.ClientFactory;
import io.zbus.transport.EventLoop;
import io.zbus.transport.Pool;
import io.zbus.transport.ServerAddress;

public class MqClientPool implements Closeable {
	private Pool<MqClient> pool; 
	private MqClientFactory factory; 
	 
	private ServerAddress serverAddress;
	private int clientPoolSize;
	private EventLoop loop;    
	
	public MqClientPool(String address, int clientPoolSize, EventLoop loop){    
		this.serverAddress = new ServerAddress(address, this.loop.isSslEnabled());
		buildPool(this.serverAddress, clientPoolSize, loop);
	}
	
	public MqClientPool(String serverAddress, int clientPoolSize){
		this(serverAddress, clientPoolSize, null);
	}
	
	public MqClientPool(String serverAddress){
		this(serverAddress, 64);
	}  
	
	public MqClientPool(ServerAddress serverAddress, int clientPoolSize, EventLoop loop){    
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
		
		this.factory = new MqClientFactory(serverAddress, this.loop);
		this.pool = new Pool<MqClient>(factory, this.clientPoolSize);  
	}

	public MqClient borrowClient(){
		try {
			MqClient client = this.pool.borrowObject();  
			return client;
		} catch (Exception e) {
			throw new MqException(e.getMessage(), e);
		} 
	}
	
	public void returnClient(MqClient... client){
		if(client == null || client.length == 0) return;
		for(MqClient c : client){
			this.pool.returnObject(c);
		}
	}
	
	public MqClient createClient(){
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
	
	private static class MqClientFactory extends ClientFactory<Message, Message, MqClient>{ 
		private EventLoop loop;  
		public MqClientFactory(ServerAddress serverAddress, EventLoop loop){ 
			super(serverAddress);
			this.loop = loop;
		} 
		
		public MqClient createObject() { 
			return new MqClient(serverAddress, loop);
		}  
	} 
}
