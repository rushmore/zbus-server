package io.zbus.mq.api;

public interface ConsumerHandler { 
	
	String topic();
	
	String consumeGroup();
	
	int maxInFlightMessage();
	
	void onMessage(MqClient client, Message message);
	
	void onConsumeOk(MqClient client, Message message);
	
	void onConsumeCancelOk(MqClient client, Message message); 

	void onQuit(MqClient client, Message message);
}
