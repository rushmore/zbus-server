package io.zbus.mq.api;

import io.zbus.mq.api.MqAdmin.Auth;
import io.zbus.mq.client.TcpMqClient;
import io.zbus.net.IoDriver;

public class ConsumerExample {

	public static void main(String[] args) { 
		IoDriver ioDriver = new IoDriver();
		
		@SuppressWarnings("resource")
		final MqClient client = new TcpMqClient("localhost:8080", ioDriver);  
		client.configAuth(new Auth());
		
		client.consume(new ConsumeHandler() { 
			@Override
			public String topic() { 
				return "MyTopic";
			}
			@Override
			public String consumeGroup() {
				return null;
			}
			
			@Override
			public int maxInFlightMessage() {
				return 20;
			}
			
			@Override
			public void onQuit(MqClient client, Message message) { 
				
			}
			
			@Override
			public void onMessage(MqClient client, Message message) {
				System.out.println(message); 
				client.ack(message);
			}
			
			@Override
			public void onConsumeOk(MqClient client, Message message) {
				
			}
			
			@Override
			public void onConsumeCancelOk(MqClient client, Message message) {
				
			} 
			
		});
		 
	}

}
