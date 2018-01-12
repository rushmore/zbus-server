package io.zbus.examples.mq.consumer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.zbus.mq.Broker;
import io.zbus.mq.ConsumeGroup;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.mq.Message;
import io.zbus.mq.MessageHandler;
import io.zbus.mq.MqClient;

public class ConsumerAckExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		Broker broker = new Broker("localhost:15555");   
		
		ConsumerConfig config = new ConsumerConfig(broker);
		config.setTopic("MyTopic");  
		config.setConsumeTimeout(10, TimeUnit.SECONDS);
		
		ConsumeGroup group = new ConsumeGroup(); //ConsumeGroup default to same as topic 
		group.setAck(true);    //Enable ACK. Disabled by default
		group.setAckWindow(10);
		//group.setAckTimeout(10, TimeUnit.SECONDS); //If not set, same as ConsumeTimeout
		
		config.setConsumeGroup(group); 
		config.setMessageHandler(new MessageHandler() { 
			@Override
			public void handle(Message msg, MqClient client) throws IOException {
				System.out.println(msg);    
				 
				client.ack(msg); //If no ack, message will be consumed again after timeout
			}
		});
		
		Consumer consumer = new Consumer(config);
		consumer.start(); 
	} 
}
