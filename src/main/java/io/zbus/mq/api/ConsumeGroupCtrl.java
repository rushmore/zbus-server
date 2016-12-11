package io.zbus.mq.api;

public class ConsumeGroupCtrl {
	public String topic;
	public String consumeGroup;
	
	public Boolean deleteOnExit;
	public Boolean exclusive;
	
	public Long consumeStartOffset;
	public Long consumeStartTime;
	public Boolean consumeStartDefault;    
}
