package io.zbus.mq.api;

import io.zbus.mq.api.MqAdmin.Auth;
import io.zbus.mq.client.TcpMqClient;
import io.zbus.net.IoDriver;

public class ConsumerExample {

	public static void main(String[] args) { 
		IoDriver ioDriver = new IoDriver();
		
		@SuppressWarnings("resource")
		Consumer consumer = new TcpMqClient("localhost:8080", ioDriver);  
		consumer.configAuth(new Auth());   
		 
		
		consumer.declareTopic("MyTopic");   
		Channel group = new Channel("MyTopic");  
		group.setMaxInFlight(10);  
		
		ConsumeHandler consumeHandler = new ConsumeHandler() { 
			@Override
			public void onQuit(MqClient client, Channel consumeGroup, Message message) {
				
			}
			
			@Override
			public void onMessage(MqClient client, Channel consumeGroup, Message message) {
				
			}
		}; 
		
		
		consumer.consume(group, consumeHandler);  
	}

}
