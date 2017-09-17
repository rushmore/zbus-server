package io.zbus.rpc.bootstrap;

import java.io.IOException;
import java.util.Set;

import io.zbus.kit.ClassKit;
import io.zbus.kit.StrKit;
import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.mq.Protocol;
import io.zbus.mq.server.MqServer;
import io.zbus.mq.server.MqServerConfig;
import io.zbus.rpc.Remote;
import io.zbus.rpc.RpcProcessor;
import io.zbus.transport.ServerAddress;

/**
 * Bootstrap to setup Rpc Service
 * Spring friendly setters
 * 
 * @author Rushmore
 *
 */
public class ServiceBootstrap extends AbstractBootstrap{ 
	protected MqServerConfig serverConfig = null;
	protected ConsumerConfig consumerConfig = new ConsumerConfig(); 
	
	protected MqServer mqServer = null; 
	protected Consumer consumer;
	protected RpcProcessor processor = new RpcProcessor(); 
	protected boolean autoDiscover = false;
	
	public ServiceBootstrap serviceAddress(ServerAddress... tracker){
		return (ServiceBootstrap)super.serviceAddress(tracker);
	}
	
	public ServiceBootstrap serviceAddress(String tracker){
		return (ServiceBootstrap)super.serviceAddress(tracker);
	} 
	
	@Override
	public ServiceBootstrap broker(Broker broker) { 
		return (ServiceBootstrap)super.broker(broker);
	}
	
	public ServiceBootstrap port(int port){
		if(serverConfig == null){
			serverConfig = new MqServerConfig();
		}
		serverConfig.setServerPort(port);
		return this;
	} 
	 
	public ServiceBootstrap host(String host){
		if(serverConfig == null){
			serverConfig = new MqServerConfig();
		}
		serverConfig.setServerHost(host);
		return this;
	}  
	 
	
	public ServiceBootstrap ssl(String certFile, String keyFile){
		if(serverConfig == null){
			serverConfig = new MqServerConfig();
		}
		serverConfig.setSslCertFile(certFile);
		serverConfig.setSslKeyFile(keyFile);
		serverConfig.setSslEnabled(true);
		return this;
	}  
	 
	public ServiceBootstrap autoDiscover(boolean autoDiscover) {
		this.autoDiscover = autoDiscover;
		return this;
	}
	
	
	public ServiceBootstrap storePath(String mqPath){
		if(serverConfig == null){
			serverConfig = new MqServerConfig();
		}
		serverConfig.setMqPath(mqPath);
		return this;
	}    
	
	public ServiceBootstrap serviceName(String topic){
		consumerConfig.setTopic(topic);
		return this;
	}
	
	public ServiceBootstrap serviceMask(int mask){
		consumerConfig.setTopicMask(mask);
		return this;
	}
	
	public ServiceBootstrap serviceToken(String token){  
		consumerConfig.setToken(token);
		return this;
	} 
	
	public ServiceBootstrap connectionCount(int connectionCount){ 
		consumerConfig.setConnectionCount(connectionCount);
		return this;
	} 
	
	private void validate(){
		String topic = consumerConfig.getTopic();
		if(StrKit.isEmpty(topic)){
			throw new IllegalStateException("serviceName required");
		}
		if(serverConfig == null && brokerConfig.getTrackerList().isEmpty()){
			throw new IllegalStateException("serviceAddress is missing");
		}
	}
	
	protected void initProcessor(){  
		Set<Class<?>> classes = ClassKit.scan(Remote.class);
		for(Class<?> clazz : classes){
			processor.addModule(clazz);
		}  
	}
	 
	public ServiceBootstrap start() throws Exception{
		validate(); 
		if(serverConfig != null){
			String token = consumerConfig.getToken();
			if(token != null){
				serverConfig.addToken(token, consumerConfig.getTopic());
				serverConfig.getAuthProvider().setEnabled(true); //enable auth
			} 
			mqServer = new MqServer(serverConfig); 
			mqServer.start();
			broker = new Broker(mqServer, token);  
		} else {
			broker = new Broker(brokerConfig);
		} 
		
		if(autoDiscover){
			initProcessor();
		}
		
		consumerConfig.setBroker(broker);  
		Integer mask = consumerConfig.getTopicMask();
		if(mask == null) {
			mask = Protocol.MASK_MEMORY ;
		}  
		   
		consumerConfig.setTopicMask(mask | Protocol.MASK_RPC); 
		consumerConfig.setMessageHandler(processor);     
		consumer = new Consumer(consumerConfig);
		
		consumer.start();
		return this;
	}  
	
	public ServiceBootstrap addModule(Class<?>... clazz){
		processor.addModule(clazz);
		return this;
	}  
	
	public ServiceBootstrap addModule(String module, Object... services){
		processor.addModule(module, services);
		return this;
	}
	
	public ServiceBootstrap addModule(Object... services){
		processor.addModule(services);
		return this;
	}
	
	
	@Override
	public void close() throws IOException { 
		if(consumer != null){
			consumer.close();
			consumer = null;
		}
		
		if(broker != null){
			broker.close();
			broker = null;
		}
		
		if(mqServer != null){
			mqServer.close();
		}
	}   
}
