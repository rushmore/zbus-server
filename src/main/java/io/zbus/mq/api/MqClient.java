package io.zbus.mq.api;

/**
 * 
 * Administrator to MQ, Topic/ConsumeGroup declare,remove, query
 * 
 * API works in asynchronous way. 
 * Ack -- when ack message from zbus server
 * Data -- when data message from zbus server
 * Ctrl -- when control message from zbus server, such as exit command
 * 
 * @author Rushmore
 *
 */
public interface MqClient{   
	
	MqFuture<Topic> declareTopic(TopicCtrl topic); 
	MqFuture<Boolean> removeTopic(String topicName);  
	MqFuture<Topic> queryTopic(String topicName); 
    
	MqFuture<ConsumeGroup> declareConsumeGroup(ConsumeGroupCtrl group); 
	MqFuture<Boolean> removeConsumeGroup(String topicName, String groupName);  
	MqFuture<ConsumeGroup> queryConsumeGroup(String topicName, String groupName); 
     
    void onAck(AckMessageHandler handler); 
	void onData(DataMessageHandler handler);  
	void onCtrl(CtrlMessageHandler handler); 
	
	
	MqFuture<Message> produce(Message message); 
	MqFuture<Void> consume(ConsumeGroupCtrl ctrl); 
	MqFuture<Void> route(String peerId, Message message); 
	
	Message take(int timeout);  
	
	void start();
	
	
	public static interface AckMessageHandler {
		void onAck(String cmd, Message message);
	}

	public static interface CtrlMessageHandler {
		void onCtrl(String cmd, Message message);
	}

	public static interface DataMessageHandler {
		void onData(Message message);
	}
}