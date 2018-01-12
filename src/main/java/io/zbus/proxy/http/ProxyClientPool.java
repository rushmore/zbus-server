package io.zbus.proxy.http;
 
import java.io.Closeable;
import java.io.IOException;

import io.zbus.mq.MqException;
import io.zbus.transport.ClientFactory;
import io.zbus.transport.EventLoop;
import io.zbus.transport.Pool;
import io.zbus.transport.ServerAddress;
import io.zbus.transport.http.Message;

public class ProxyClientPool implements Closeable {
	private Pool<ProxyClient> pool; 
	private ProxyClientFactory factory;  
	private ServerAddress serverAddress;
	private int clientPoolSize; 
	
	public ProxyClientPool(String address, int clientPoolSize, EventLoop loop){    
		this.serverAddress = new ServerAddress(address, loop.isSslEnabled()); 
		this.clientPoolSize = clientPoolSize;  
		this.factory = new ProxyClientFactory(serverAddress, loop);
		this.pool = new Pool<ProxyClient>(factory, this.clientPoolSize);  
	}

	public ProxyClient borrowClient(){
		try {
			ProxyClient client = this.pool.borrowObject();  
			return client;
		} catch (Exception e) {
			throw new MqException(e.getMessage(), e);
		} 
	}
	
	public void returnClient(ProxyClient... client){
		if(client == null || client.length == 0) return;
		for(ProxyClient c : client){
			this.pool.returnObject(c);
		}
	}
	
	public ProxyClient createClient(){
		return factory.createObject();
	} 
	
	@Override
	public void close() throws IOException {
		if(this.pool != null){
			this.pool.close();   
		} 
	}
	
	private static class ProxyClientFactory extends ClientFactory<Message, Message, ProxyClient>{ 
		private EventLoop loop;  
		public ProxyClientFactory(ServerAddress address, EventLoop loop){ 
			super(address);
			this.loop = loop;
		} 
		
		public ProxyClient createObject() { 
			return new ProxyClient(serverAddress.address, loop);
		}  
		
		@Override
		public boolean validateObject(ProxyClient client) {
			return client.hasConnected();
		}
	} 
}
