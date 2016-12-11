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
	
	MqFuture<Topic> declareTopic(TopicCtrl topic);
	
	MqFuture<Boolean> removeTopic(String topicName); 
	
	MqFuture<Topic> queryTopic(String topicName); 
    
	MqFuture<ConsumeGroup> declareConsumeGroup(ConsumeGroupCtrl group);
	
	MqFuture<Boolean> removeConsumeGroup(String topicName, String groupName); 
	
	MqFuture<ConsumeGroup> queryConsumeGroup(String topicName, String groupName); 
    
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
}