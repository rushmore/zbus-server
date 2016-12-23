package io.zbus.mq.api;

public interface Consumer extends MqAdmin { 
	MqFuture<ConsumeResult> subscribe(Channel channel, ConsumeHandler handler);  
	MqFuture<ConsumeResult> ready(Channel channel);  
	
	MqFuture<ConsumeResult> unsubscribe(String topic, String channel);  
	MqFuture<ConsumeResult> unsubscribe(String topic);  
	
	public static class ConsumeResult {
		public boolean sendOk;
		public boolean ackEnabled;
		public Long serverAckTime;
		public Throwable error;
	}
}
