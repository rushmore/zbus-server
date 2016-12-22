package io.zbus.mq.api;

public interface Consumer extends MqAdmin { 
	MqFuture<ConsumeResult> consume(ConsumerHandler handler);    
	MqFuture<ConsumeResult> cancelConsume(String topic, String consumeGroup);  
	MqFuture<ConsumeResult> cancelConsume(String topic);  
	void ack(Message message);
	
	public static class ConsumeOption {
		public String topic;
		public String consumeGroup;  
		public int window = 1;
		public Boolean ack;
	}
	
	public static class ConsumeResult {
		public boolean sendOk;
		public boolean ackEnabled;
		public Long serverAckTime;
		public Throwable error;
	}
}
