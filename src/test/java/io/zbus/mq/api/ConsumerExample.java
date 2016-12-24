package io.zbus.mq.api;

import io.zbus.mq.api.Consumer.ChannelContext;
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
		
		ConsumeHandler consumeHandler = new ConsumeHandler() {
			@Override
			public void onQuit(ChannelContext ctx, Message message) {
				
			}
			
			@Override
			public void onMessage(ChannelContext ctx, Message message) {   
				
				
				ctx.client.ack(message.getId(), message.getOffset());
			}
		}; 
		
		consumer.subscribe("MyTopic", consumeHandler);  
		consumer.unsubscribe("MyTopic");
	}

}
