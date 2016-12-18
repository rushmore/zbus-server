package io.zbus.mq.api;
 
public interface MqClient extends MqAdmin{    
     
    void onAck(AckMessageHandler handler); 
	void onData(DataMessageHandler handler);  
	void onCtrl(CtrlMessageHandler handler); 
	
	MqFuture<ProduceResult> produce(Message message); 
	MqFuture<ConsumeResult> consume(ConsumeCtrl ctrl);   
	Message take(int timeout);   
	
	public static interface AckMessageHandler {
		void onAck(Message message);
	}

	public static interface CtrlMessageHandler {
		void onCtrl(String cmd, Message message);
	}

	public static interface DataMessageHandler {
		void onData(Message message);
	}
}