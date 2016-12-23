package io.zbus.mq.api;

public interface Consumer extends MqAdmin { 
	MqFuture<ConsumeResult> consume(ConsumeGroup consumeGroup, ConsumeHandler handler);  
	MqFuture<ConsumeResult> ready(ConsumeGroup consumeGroup);  
	
	MqFuture<ConsumeResult> cancelConsume(String topic, String consumeGroup);  
	MqFuture<ConsumeResult> cancelConsume(String topic);  
	
	public static class ConsumeResult {
		public boolean sendOk;
		public boolean ackEnabled;
		public Long serverAckTime;
		public Throwable error;
	}
}
