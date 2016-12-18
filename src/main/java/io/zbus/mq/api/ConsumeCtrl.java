package io.zbus.mq.api;

public class ConsumeCtrl {
	public String topic;
	public String channel; 
	
	public String messageFilter;
	public Long consumeStartOffset; //valid only for single MqClient
	public Long consumeStartTime;
	public Boolean consumeStartDefault;    
}
