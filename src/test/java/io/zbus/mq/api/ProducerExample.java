package io.zbus.mq.api;

import io.zbus.mq.api.MqAdmin.AckMessageHandler;
import io.zbus.mq.api.MqAdmin.CtrlMessageHandler;

public class ProducerExample {

	public static void main(String[] args) { 
		
		Producer producer = new Producer(null);   
		producer.setAppId("app-id");
		producer.setToken("app-token");
		
		producer.onData(new MessageHandler() {  
			@Override
			public void onMessage(Message message) { 
				
			}
		});
		
		producer.onAck(new AckMessageHandler() { 
			@Override
			public void onAck(String msgId, Message message) { 
				
			}
		});
		
		producer.onCtrl(new CtrlMessageHandler() { 
			@Override
			public void onCtrl(String cmd, Message message) {
				
			}
		}); 
		
		
		Message message = new Message();
		message.setAck(true); 
		message.setBody("hello world");
		
		producer.send(message);  
	}

}
