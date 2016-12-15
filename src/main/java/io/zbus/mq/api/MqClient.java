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
	
	MqFuture<Topic> declareTopic(TopicCtrl ctrl); 
	MqFuture<Boolean> removeTopic(String topic);  
	MqFuture<Topic> queryTopic(String topic); 
    
	MqFuture<Channel> declareChannel(ChannelCtrl ctrl); 
	MqFuture<Boolean> removeChannel(String topic, String channel);  
	MqFuture<Channel> queryChannel(String topic, String channel); 
     
    void onAck(AckMessageHandler handler); 
	void onData(DataMessageHandler handler);  
	void onCtrl(CtrlMessageHandler handler); 
	
	
	MqFuture<Message> produce(Message message); 
	MqFuture<Void> consume(ChannelCtrl ctrl);  
	
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