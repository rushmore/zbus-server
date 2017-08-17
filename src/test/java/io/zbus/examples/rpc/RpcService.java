package io.zbus.examples.rpc;

import io.zbus.examples.rpc.biz.BaseExtImpl;
import io.zbus.examples.rpc.biz.InterfaceExampleImpl;
import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.rpc.RpcProcessor;

public class RpcService {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		//With RpcProcessor, we only care about the moudles and bussiness logic, nothing related to zbus
		RpcProcessor processor = new RpcProcessor();
		processor.addModule(new InterfaceExampleImpl()); //By default interface full name, empty are used as module name
		//processor.addModule(module, services); //You can define module name, it is optional
		processor.addModule(new BaseExtImpl());
		
		
		//The following is same as a simple Consumer setup process
		Broker broker = new Broker("localhost:15555");   
		//Broker broker = new Broker("localhost:15555;localhost:15556"); //Why not test HA?
		ConsumerConfig config = new ConsumerConfig(broker); 
		config.setTopic("MyRpc");
		config.setMessageHandler(processor);   
		
		Consumer consumer = new Consumer(config); 
		
		consumer.start(); 
	} 
}
