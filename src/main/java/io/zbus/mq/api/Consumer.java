package io.zbus.mq.api;


public interface Consumer extends MqAdmin {    
	void start(ConsumeCtrl ctrl);   
	
	void pause(String topic, String channel);  
	
	void onData();
}
