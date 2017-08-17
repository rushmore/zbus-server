package io.zbus.transport;

import java.io.IOException;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.transport.Pool.ObjectFactory;

public abstract class ClientFactory<REQ extends Id, RES extends Id, T extends Client<REQ, RES>> 
	implements ObjectFactory<T> { 
	private static final Logger log = LoggerFactory.getLogger(ClientFactory.class); 
	
	protected final ServerAddress serverAddress; 
	
	public ClientFactory(ServerAddress serverAddress){
		this.serverAddress = serverAddress; 
	}
	
	public ServerAddress getServerAddress(){
		return serverAddress;
	}
	
	@Override
	public boolean validateObject(T client) { 
		if(client == null) return false;
		return client.hasConnected();
	}
	
	@Override
	public void destroyObject(T client){ 
		try {
			client.close();
		} catch (IOException e) {
			log.error(e.getMessage(), e); 
		}
	} 
	 
	public abstract T createObject();
}
