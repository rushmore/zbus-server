package io.zbus.mq.api;

public interface Consumer extends MqAdmin { 
	MqFuture<ConsumeResult> consume(ConsumeGroup consumeGroup);    
	MqFuture<ConsumeResult> cancelConsume(String topic, String consumeGroup);  
	MqFuture<ConsumeResult> cancelConsume(String topic);  
	void ack(Message message);
	
	public static class ConsumeResult {
		public boolean sendOk;
		public boolean ackEnabled;
		public Long serverAckTime;
		public Throwable error;
	}
}
