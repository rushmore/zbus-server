package io.zbus.rpc.bootstrap.mq;

import java.io.Closeable;
import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.BrokerConfig;
import io.zbus.mq.ProducerConfig;
import io.zbus.rpc.RpcConfig;
import io.zbus.rpc.RpcInvoker;
import io.zbus.rpc.transport.mq.RpcMessageInvoker;
import io.zbus.transport.ServerAddress;

public class ClientBootstrap implements Closeable {   
	protected BrokerConfig brokerConfig = null;  
	protected Broker broker; 
	
	protected RpcConfig rpcConfig = new RpcConfig(); 
	protected ProducerConfig producerConfig = new ProducerConfig();
	protected String topic; 
	protected boolean requestTypeInfo = true; 
	
	public ClientBootstrap requestTypeInfo(boolean requestTypeInfo){  
		this.requestTypeInfo = requestTypeInfo;
		return this;
	}    
	
	public ClientBootstrap serviceName(String topic){
		this.topic = topic;
		return this;
	}
	 
	public ClientBootstrap serviceToken(String token){  
		producerConfig.setToken(token); 
		return this;
	}    
	
	public RpcInvoker invoker(){
		if(broker == null){
			String token = producerConfig.getToken();
			if(token != null){
				for(ServerAddress address : brokerConfig.getTrackerList()){
					if(address.getToken() == null){
						address.setToken(token);
					}
				}
			}
			broker = new Broker(brokerConfig);
		} 
		producerConfig.setBroker(broker);
		RpcMessageInvoker messageInvoker = new RpcMessageInvoker(producerConfig, this.topic);
		rpcConfig.setMessageInvoker(messageInvoker);
		RpcInvoker rpcInvoker = new RpcInvoker(rpcConfig); 
		rpcInvoker.getCodec().setRequestTypeInfo(requestTypeInfo); 
		
		return rpcInvoker;
	}
	
	public void module(String module){
		this.rpcConfig.setModule(module);
	}
	
	public <T> T createProxy(Class<T> clazz){  
		return invoker().createProxy(clazz); 
	}    
	
	 
	public ClientBootstrap broker(Broker broker){
		this.broker = broker;
		return this;
	}
	
	public ClientBootstrap serviceAddress(ServerAddress... tracker){
		if(brokerConfig == null){
			brokerConfig = new BrokerConfig();
		}
		for(ServerAddress address : tracker){
			brokerConfig.addTracker(address);
		}
		return this;
	}
	
	public ClientBootstrap serviceAddress(String tracker){
		String[] bb = tracker.split("[;, ]");
		for(String addr : bb){
			addr = addr.trim();
			if("".equals(addr)) continue;
			ServerAddress serverAddress = new ServerAddress(addr);
			serviceAddress(serverAddress);
		}
		return this;
	} 
	
	@Override
	public void close() throws IOException { 
		if(broker != null){
			broker.close();
			broker = null;
		}
	} 
}
