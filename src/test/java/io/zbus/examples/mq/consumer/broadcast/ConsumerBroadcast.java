package io.zbus.examples.mq.consumer.broadcast;

import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.mq.Message;
import io.zbus.mq.MessageHandler;
import io.zbus.mq.MqClient;

public class ConsumerBroadcast {
 
	public static void main(String[] args) throws Exception {   
		Broker broker = new Broker("localhost:15555");    
		
		//Create 10 consumers, of each consumes on different ConsumeGroup of the same Topic(MyTopic)
		//For every message produced, you should see 10 messages consumed(printed) in the console
		
		Consumer[] consumers = new Consumer[10];
		for(int i=0; i<consumers.length;i++){
			ConsumerConfig config = new ConsumerConfig(broker);
			config.setTopic("MyTopic");     
			config.setConsumeGroup("Broadcast" + i); //ConsumeGroup name 
			config.setConnectionCount(1);            //Demo only 1 connection for each consumer
			config.setMessageHandler(new MessageHandler() {  
				@Override
				public void handle(Message msg, MqClient client) throws IOException { 
					System.out.println(msg);
				}
			});
			
			consumers[i] = new Consumer(config);
			
		}
		for(Consumer c : consumers){
			c.start();
		}
	} 
}
