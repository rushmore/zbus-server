package io.zbus.mq.api;

public class ProducerExample {

	public static void main(String[] args) { 
		
		Producer producer = null;
		
		producer.onData(new DataMessageHandler() {  
			@Override
			public void onData(Message message) { 
				
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
