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
public interface MqAdmin{   
	
	Future<Topic> declareTopic(TopicCtrl topic);
	
	Future<Boolean> removeTopic(String topicName); 
	
	Future<Topic> queryTopic(String topicName); 
    
	Future<ConsumeGroup> declareConsumeGroup(ConsumeGroupCtrl group);
	
	Future<Boolean> removeConsumeGroup(String topicName, String groupName); 
	
	Future<ConsumeGroup> queryConsumeGroup(String topicName, String groupName); 
    
	/**
	 * Setup Ack Message handler, Ack message notified with request message id back
	 * 
	 * @param handler AckMessageHandler
	 */
    void onAck(AckMessageHandler handler);
    /**
     * 
     * 
     * @param handler
     */
	void onData(DataMessageHandler handler); 
	
	void onCtrl(CtrlMessageHandler handler); 
	
	void route(String peerId, Message message);
	
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