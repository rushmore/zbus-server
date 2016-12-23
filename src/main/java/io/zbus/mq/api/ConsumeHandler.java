package io.zbus.mq.api;

import io.zbus.mq.api.MqClient.ChannelContext;

public interface ConsumeHandler { 
	
	void onMessage(ChannelContext ctx, Message message); 

	void onQuit(ChannelContext ctx, Message message);
}
