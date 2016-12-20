package io.zbus.mq.api;

import io.zbus.mq.api.MqClient.MqFuture;

public interface MqAdmin{   
	
	MqFuture<Topic> declareTopic(TopicCtrl ctrl); 
	MqFuture<Boolean> removeTopic(String topic);  
	MqFuture<Topic> queryTopic(String topic); 
    
	MqFuture<Channel> declareChannel(ChannelCtrl ctrl); 
	MqFuture<Boolean> removeChannel(String topic, String channel);  
	MqFuture<Channel> queryChannel(String topic, String channel);   
	
	public static class Topic {
		public String name; 
	}
	
	public static class TopicCtrl {
		public String name;  
	}
	
	public static class Channel {
		public String topic;
		public String channel; 
	}
	
	public static class ChannelCtrl {
		public String topic;
		public String channel;
		
		public Boolean deleteOnExit;
		public Boolean exclusive;
		
		public Long consumeStartOffset;
		public Long consumeStartTime;
		public Boolean consumeStartDefault;    
	} 
}