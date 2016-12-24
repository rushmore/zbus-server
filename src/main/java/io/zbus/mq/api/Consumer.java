package io.zbus.mq.api;

public interface Consumer extends MqAdmin { 
	MqFuture<ConsumeResult> subscribe(String topic, String channel, int maxInFlight, ConsumeHandler handler);  
	MqFuture<ConsumeResult> subscribe(String topic, int maxInFlight, ConsumeHandler handler);
	MqFuture<ConsumeResult> subscribe(String topic, ConsumeHandler handler);
	MqFuture<ConsumeResult> ready(String topic, String channel, int count);  
	
	MqFuture<ConsumeResult> unsubscribe(String topic, String channel);  
	MqFuture<ConsumeResult> unsubscribe(String topic);  
	
	public static class ConsumeResult {
		public boolean sendOk;
		public boolean ackEnabled;
		public Long serverAckTime;
		public Throwable error;
	}
	
	public static class ChannelContext{
		public final String topic;
		public final String channel;
		public final ConsumeHandler handler;
		public final MqClient client;
		public int maxInFlight;
		
		public ChannelContext(String topic, String channel, ConsumeHandler handler, MqClient client) {
			this.topic = topic;
			this.channel = channel;
			this.handler = handler;
			this.client = client;
		}
	} 
}
