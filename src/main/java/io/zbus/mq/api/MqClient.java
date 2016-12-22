package io.zbus.mq.api;

import java.io.Closeable;

public interface MqClient extends MqAdmin, Closeable{     
	MqFuture<ProduceResult> produce(Message message); 
	MqFuture<ConsumeResult> consume(ConsumerHandler handler);    
	MqFuture<ConsumeResult> cancelConsume(String topic, String consumeGroup);  
	MqFuture<ConsumeResult> cancelConsume(String topic);  
	void ack(Message message);
	
	public static class ProduceResult {
		public boolean sendOk;
		public boolean ackEnabled;
		public Long serverAckTime;
		public Throwable error;
	}
	
	public static class ConsumeCtrl {
		public String topic;
		public String consumeGroup; 
		public ConsumerHandler handler;
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