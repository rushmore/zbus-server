package io.zbus.mq.api;
 
public interface MqAdmin{   
	
	MqFuture<Topic> declareTopic(TopicCtrl ctrl); 
	MqFuture<Boolean> removeTopic(String topic);  
	MqFuture<Topic> queryTopic(String topic); 
    
	MqFuture<Channel> declareChannel(ChannelCtrl ctrl); 
	MqFuture<Boolean> removeChannel(String topic, String channel);  
	MqFuture<Channel> queryChannel(String topic, String channel);   
	
}