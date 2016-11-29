package io.zbus.mq.api;

public interface Producer extends MqAdmin{ 
	void send(Message message); 
}
