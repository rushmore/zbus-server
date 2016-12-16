package io.zbus.mq.api;

public interface Producer extends MqAdmin {
	MqFuture<Message> publish(Message message); 
}
