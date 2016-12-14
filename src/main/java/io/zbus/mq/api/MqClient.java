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
	
	Future<Topic> declareTopic(TopicCtrl topic); 
	Future<Boolean> removeTopic(String topicName);  
	Future<Topic> queryTopic(String topicName); 
    
	Future<ConsumeGroup> declareConsumeGroup(ConsumeGroupCtrl group); 
	Future<Boolean> removeConsumeGroup(String topicName, String groupName);  
	Future<ConsumeGroup> queryConsumeGroup(String topicName, String groupName); 
     
    void onAck(AckMessageHandler handler); 
	void onData(DataMessageHandler handler);  
	void onCtrl(CtrlMessageHandler handler); 
	
	
	Future<Message> produce(Message message); 
	void consume(ConsumeGroupCtrl ctrl);
	
	void route(String peerId, Message message); 
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