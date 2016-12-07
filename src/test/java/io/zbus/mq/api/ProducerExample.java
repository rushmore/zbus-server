package io.zbus.mq.api;

public class ProducerExample {

	public static void main(String[] args) { 
		
		Producer producer = new Producer(null);   
		producer.setAppId("app-id");
		producer.setToken("app-token");
		
		Message message = new Message();
		message.setBody("hello world");
		
		producer.put(message);  
	}

}
