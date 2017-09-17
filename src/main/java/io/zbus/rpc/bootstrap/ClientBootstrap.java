package io.zbus.rpc.bootstrap;

import io.zbus.mq.Broker;
import io.zbus.rpc.RpcConfig;
import io.zbus.rpc.RpcInvoker;
import io.zbus.transport.ServerAddress;

public class ClientBootstrap extends AbstractBootstrap{   
	protected RpcConfig rpcConfig = new RpcConfig(); 
	
	public ClientBootstrap serviceName(String topic){
		rpcConfig.setTopic(topic); 
		return this;
	}
	 
	public ClientBootstrap serviceToken(String token){  
		rpcConfig.setToken(token);
		return this;
	}  
	
	public RpcInvoker invoker(){
		if(broker == null){
			String token = rpcConfig.getToken();
			if(token != null){
				for(ServerAddress address : brokerConfig.getTrackerList()){
					if(address.getToken() == null){
						address.setToken(token);
					}
				}
			}
			broker = new Broker(brokerConfig);
		} 
		rpcConfig.setBroker(broker);
		return new RpcInvoker(rpcConfig); 
	}
	
	public <T> T createProxy(Class<T> clazz){  
		return invoker().createProxy(clazz); 
	}    
	
	
	@Override
	public ClientBootstrap broker(Broker broker) { 
		return (ClientBootstrap)super.broker(broker);
	}
	
	public ClientBootstrap serviceAddress(ServerAddress... tracker){
		return (ClientBootstrap)super.serviceAddress(tracker);
	}
	
	public ClientBootstrap serviceAddress(String tracker){
		return (ClientBootstrap)super.serviceAddress(tracker);
	}
	
}
