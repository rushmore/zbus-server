package io.zbus.examples.mq.consumer.multicast;

import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.mq.Message;
import io.zbus.mq.MessageHandler;
import io.zbus.mq.MqClient;

//MyTopic + MutilcastGroup1
public class ConsumerInGroup1 {
 
	public static void main(String[] args) throws Exception {   
		Broker broker = new Broker("localhost:15555");    
		
		//Create 2 consumers, consuming on same ConsumeGroup of Topic(MyTopic)
		//the two consumers in the same ConsumeGroup are loadblancing
		
		Consumer[] consumers = new Consumer[2];
		for(int i=0; i<consumers.length;i++){
			ConsumerConfig config = new ConsumerConfig(broker);
			config.setTopic("MyTopic");     
			config.setConsumeGroup("MutilcastGroup1"); //ConsumeGroup name 
			config.setConnectionCount(1);             
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
