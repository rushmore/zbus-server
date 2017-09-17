package io.zbus.examples.mq.consumer.msgfilter;

import java.io.IOException;

import io.zbus.mq.Broker;
import io.zbus.mq.ConsumeGroup;
import io.zbus.mq.MessageHandler;
import io.zbus.mq.Consumer;
import io.zbus.mq.ConsumerConfig;
import io.zbus.mq.Message;
import io.zbus.mq.MqClient;
 

public class ConsumerGroupFilter1 {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		Broker broker = new Broker("localhost:15555");    
		
		ConsumerConfig config = new ConsumerConfig(broker);
		config.setTopic("MyTopic");        
		ConsumeGroup group = new ConsumeGroup();
		group.setGroupName("FilterGroup1");
		group.setFilter("Stock.HK.#");  //Filter is ConsumeGroup wide, it influence other consumers on same ConsumeGroup
		
		config.setConsumeGroup(group); 
		
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
