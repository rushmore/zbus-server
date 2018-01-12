package io.zbus.rpc.bootstrap.mq;

import io.zbus.transport.ServerAddress;

public class SpringClientBootstrap extends ClientBootstrap {
	
	public void setServiceAddress(ServerAddress... tracker){
		serviceAddress(tracker);
	}
	
	public void setRequestTypeInfo(boolean requestTypeInfo){
		requestTypeInfo(requestTypeInfo);
	}
	
	public void setServiceAddress(String tracker){
		serviceAddress(tracker);
	}
	
	public void setServiceName(String topic){
		serviceName(topic);
	}
	 
	public void setServiceToken(String token){  
		serviceToken(token);
	}   
}
