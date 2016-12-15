package io.zbus.mq.api;

public class ChannelCtrl {
	public String topic;
	public String channel;
	
	public Boolean deleteOnExit;
	public Boolean exclusive;
	
	public Long consumeStartOffset;
	public Long consumeStartTime;
	public Boolean consumeStartDefault;    
}
