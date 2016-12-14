package io.zbus.mq.api;

public interface Producer extends MqAdmin{ 
	
	Future<Message> send(Message message); 
	
}