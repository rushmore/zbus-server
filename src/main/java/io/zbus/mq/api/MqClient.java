package io.zbus.mq.api;

import java.io.Closeable;

public interface MqClient extends MqAdmin, Closeable{    
     
    void onProduceAck(AckHandler handler); 
    void onConsumeAck(AckHandler handler);  
	void onStream(StreamHandler handler);  
	void onQuit(QuitHandler handler); 
	
	MqFuture<ProduceResult> produce(Message message); 
	MqFuture<ConsumeResult> consume(ConsumeCtrl ctrl);   
	Message take(int timeout);   
	
	
	
	public static interface AckHandler {
		void onAck(Message message);
	} 

	public static interface StreamHandler {
		void onStream(Message message);
	} 
	
	public static interface QuitHandler {
		void onQuit(Message message);
	}
	
	public static class ProduceResult {
		public boolean sendOk;
		public boolean ackEnabled;
		public Long serverAckTime;
		public Throwable error;
	}
	
	public static class ConsumeCtrl {
		public String topic;
		public String channel; 
		
		public String messageFilter;
		public Long consumeStartOffset; //valid only for single MqClient
		public Long consumeStartTime;
		public Boolean consumeStartDefault;    
	}
	
	public static class ConsumeResult {
		public boolean sendOk;
		public boolean ackEnabled;
		public Long serverAckTime;
		public Throwable error;
	}
}