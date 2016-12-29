package io.zbus.mq.api;

public interface Producer extends MqAdmin {  
	Future<ProduceResult> produce(Message message);
	
	public static class ProduceResult {
		public boolean sendOk;
		public boolean ackEnabled;
		public Long serverAckTime;
		public Throwable error;
	} 
}
