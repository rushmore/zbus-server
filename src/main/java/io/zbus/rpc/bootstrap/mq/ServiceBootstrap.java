package io.zbus.rpc.bootstrap.mq;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

import io.zbus.kit.ClassKit;
import io.zbus.kit.StrKit;
import io.zbus.mq.Broker;
import io.zbus.mq.BrokerConfig;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.mq.MessageHandler;
import io.zbus.mq.Protocol;
import io.zbus.mq.Topic;
import io.zbus.mq.server.MqServer;
import io.zbus.mq.server.MqServerConfig;
import io.zbus.rpc.Remote;
import io.zbus.rpc.RpcProcessor;
import io.zbus.rpc.transport.mq.RpcMessageHandler;
import io.zbus.transport.ServerAddress;

public class ServiceBootstrap implements Closeable{ 
	protected BrokerConfig brokerConfig = null;  
	protected Broker broker; 
	
	protected MqServerConfig serverConfig = null;
	protected ConsumerConfig consumerConfig = new ConsumerConfig(); 
	
	protected MqServer mqServer = null; 
	protected Consumer consumer;
	protected RpcProcessor processor = new RpcProcessor(); 
	protected boolean autoDiscover = false;
	protected boolean verbose = false;
	  
	
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
	
	public ServiceBootstrap verbose(boolean verbose) {
		this.verbose = verbose;
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
		processor.setDocUrlContext("/"+topic+"/");
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
	
	public ServiceBootstrap responseTypeInfo(boolean responseTypeInfo){  
		processor.getCodec().setResponseTypeInfo(responseTypeInfo);
		return this;
	}   
	
	public ServiceBootstrap stackTrace(boolean stackTrace) {
		this.processor.setEnableStackTrace(stackTrace);
		return this;
	} 
	
	public ServiceBootstrap methodPage(boolean methodPage) {
		this.processor.setEnableMethodPage(methodPage);
		return this;
	} 
	
	/**
	 * If topic(ServiceName) in zbus is missing, should we declare it or not.
	 * 
	 * @param declareOnMissing
	 * @return
	 */
	public ServiceBootstrap declareOnMissing(boolean declareOnMissing) {
		this.consumerConfig.setDeclareOnMissing(declareOnMissing);
		return this;
	} 
	
	private void validate(){
		Topic topic = consumerConfig.getTopic();
		if(topic == null || StrKit.isEmpty(topic.getName())){
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
				serverConfig.addToken(token, consumerConfig.getTopic().getName());
				serverConfig.getAuthProvider().setEnabled(true); //enable auth
			} 
			serverConfig.setVerbose(verbose);
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
		Integer mask = consumerConfig.getTopic().getMask();
		if(mask == null) {
			mask = Protocol.MASK_MEMORY ;
		}    
		MessageHandler rpcHandler = new RpcMessageHandler(this.processor);
		   
		consumerConfig.setTopicMask((mask | Protocol.MASK_RPC) & ~Protocol.MASK_ACK_REQUIRED); 
		consumerConfig.setMessageHandler(rpcHandler);     
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
	
	public ServiceBootstrap addModule(String module, Class<?>... clazz){
		processor.addModule(module, clazz);
		return this;
	}
	
	public ServiceBootstrap addModule(Object... services){
		processor.addModule(services);
		return this;
	}
	
	public ServiceBootstrap broker(Broker broker){
		this.broker = broker;
		return this;
	}
	
	public ServiceBootstrap serviceAddress(ServerAddress... tracker){
		if(brokerConfig == null){
			brokerConfig = new BrokerConfig();
		}
		for(ServerAddress address : tracker){
			brokerConfig.addTracker(address);
		}
		return this;
	}
	
	public ServiceBootstrap serviceAddress(String addressList){
		String[] bb = addressList.split("[;, ]");
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
