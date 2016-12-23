package io.zbus.mq.api;

public interface ConsumeHandler { 
	
	void onMessage(MqClient client, Channel consumeGroup, Message message); 

	void onQuit(MqClient client, Channel consumeGroup, Message message);
}
