package io.zbus.mq.api;

import io.zbus.mq.api.Consumer.ConsumeResult;

public interface Channel {
	 Future<ConsumeResult> consume(String channelId, ConsumeHandler handler); 
}
