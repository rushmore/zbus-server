package io.zbus.mq.api;

public interface Producer extends MqAdmin{ 
	
	MqFuture<Message> send(Message message); 
	
}