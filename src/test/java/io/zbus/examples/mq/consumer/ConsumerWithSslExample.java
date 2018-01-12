package io.zbus.examples.mq.consumer;

import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.MessageHandler;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.mq.Message;
import io.zbus.mq.MqClient;
import io.zbus.transport.ServerAddress;

public class ConsumerWithSslExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		ServerAddress serverAddress = new ServerAddress("localhost:15555", true);
		serverAddress.setCertFile("ssl/zbus.crt"); 
		
		Broker broker = new Broker(); 
		broker.addTracker(serverAddress);
		
		ConsumerConfig config = new ConsumerConfig(broker);
		config.setTopic("MyTopic");  
		config.setMessageHandler(new MessageHandler() { 
			@Override
			public void handle(Message msg, MqClient client) throws IOException {
				System.out.println(msg);
			}
		});
		
		Consumer consumer = new Consumer(config);
		consumer.start(); 
	} 
}
