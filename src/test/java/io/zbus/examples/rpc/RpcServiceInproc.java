package io.zbus.examples.rpc;

import io.zbus.examples.rpc.biz.BaseExtImpl;
import io.zbus.examples.rpc.biz.InterfaceExampleImpl;
import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.mq.server.MqServer;
import io.zbus.rpc.RpcProcessor;

public class RpcServiceInproc {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		 
		RpcProcessor processor = new RpcProcessor();
		processor.addModule(new InterfaceExampleImpl());  
		processor.addModule(new BaseExtImpl());
		
		
		MqServer server = new MqServer();  //Start zbus internally
		server.start();
		
		Broker broker = new Broker(server);     
		ConsumerConfig config = new ConsumerConfig(broker); 
		config.setTopic("MyRpc");
		config.setMessageHandler(processor);    
		Consumer consumer = new Consumer(config);  
		consumer.start(); 
	} 
}
