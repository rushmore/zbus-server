package io.zbus.rpc.bootstrap;

import java.io.Closeable;
import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.BrokerConfig;
import io.zbus.transport.ServerAddress;

public abstract class AbstractBootstrap implements Closeable{  
	protected BrokerConfig brokerConfig = null;  
	protected Broker broker; 
	
	public AbstractBootstrap broker(Broker broker){
		this.broker = broker;
		return this;
	}
	
	public AbstractBootstrap serviceAddress(ServerAddress... tracker){
		if(brokerConfig == null){
			brokerConfig = new BrokerConfig();
		}
		for(ServerAddress address : tracker){
			brokerConfig.addTracker(address);
		}
		return this;
	}
	
	public AbstractBootstrap serviceAddress(String tracker){
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
