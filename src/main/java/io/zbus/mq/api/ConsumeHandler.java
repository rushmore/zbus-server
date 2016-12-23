package io.zbus.mq.api;

public interface ConsumeHandler { 
	
	void onMessage(MqClient client, ConsumeGroup consumeGroup, Message message); 

	void onQuit(MqClient client, ConsumeGroup consumeGroup, Message message);
}
