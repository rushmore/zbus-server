package io.zbus.mq.api;

public interface MqClient extends Producer, Consumer{  
	void ack(String msgid, Long offset); 
	
	public static class ChannelContext{
		public final Channel channel;
		public final ConsumeHandler consumeHandler;
		public final MqClient mqClient;
		public ChannelContext(Channel channel, ConsumeHandler consumeHandler, MqClient mqClient) {
			this.channel = channel.clone(); 
			this.consumeHandler = consumeHandler;
			this.mqClient = mqClient;
		}
	} 
}