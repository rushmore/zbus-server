package io.zbus.mq.api;

public interface ConsumeHandler { 
	
	void onMessage(ConsumeGroup consumeGroup, Message message);
	
	void onConsumeOk(ConsumeGroup consumeGroup, Message message);
	
	void onConsumeCancelOk(ConsumeGroup consumeGroup, Message message); 

	void onQuit(ConsumeGroup consumeGroup, Message message);
}
