package io.zbus.mq.api;

public interface Consumer extends MqAdmin {  
	Future<ConsumeResult> ready(String topic, String channel, int count);  
	
	Future<ConsumeResult> unsubscribe(String topic, String channel);  
	Future<ConsumeResult> unsubscribe(String topic);  
	
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
		public boolean autoAck = false;
		
		public ChannelContext(String topic, String channel, ConsumeHandler handler, MqClient client) {
			this.topic = topic;
			this.channel = channel;
			this.handler = handler;
			this.client = client;
		}
	} 
}
