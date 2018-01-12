package io.zbus.rpc.bootstrap.http;

import io.zbus.transport.ServerAddress;

public class SpringClientBootstrap extends ClientBootstrap {
	
	public void setServiceAddress(ServerAddress serverAddress){
		serviceAddress(serverAddress);
	}
	
	public void setServiceAddress(String serverAddress){
		serviceAddress(serverAddress);
	} 
	
	public void setRequestTypeInfo(boolean requestTypeInfo){
		requestTypeInfo(requestTypeInfo);
	}
	 
	public void setServiceToken(String token){  
		serviceToken(token);
	}  
	
	public void setClientPoolSize(int clientPoolSize) {
		clientPoolSize(clientPoolSize);
	}
}
