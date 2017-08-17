package io.zbus.examples.rpc.generic;

import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.rpc.RpcProcessor;

public class RpcService {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {    
		RpcProcessor processor = new RpcProcessor();  
		processor.addModule(new GenericMethodImpl());
		
		Broker broker = new Broker("localhost:15555");    
		ConsumerConfig config = new ConsumerConfig(broker); 
		config.setTopic("Generic");
		config.setMessageHandler(processor);   
		
		Consumer consumer = new Consumer(config); 
		
		consumer.start(); 
	} 
}
