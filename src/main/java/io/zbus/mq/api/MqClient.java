package io.zbus.mq.api;

public interface MqClient extends Producer, Consumer{  
	
	void ack(String msgid, Long offset);  
}