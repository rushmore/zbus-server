package io.zbus.mq.api;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

public interface MqAdmin extends Closeable{   
	
	MqFuture<Topic> declareTopic(TopicDeclare ctrl); 
	MqFuture<Boolean> removeTopic(TopicRemove ctrl);  
	MqFuture<Topic> queryTopic(TopicQuery ctrl);  
	MqFuture<Topic> declareTopic(String topic, boolean rpcFlag); 
	MqFuture<Topic> declareTopic(String topic);
	MqFuture<Topic> queryTopic(String topic); 
	MqFuture<Boolean> removeTopic(String topic); 
    
	MqFuture<ChannelDetails> declareChannel(ChannelDeclare ctrl); 
	MqFuture<Boolean> removeChannel(ChannelRemove ctrl);  
	MqFuture<ChannelDetails> queryChannel(ChannelQuery ctrl);    
	MqFuture<ChannelDetails> declareChannel(String topic, String channel); 
	MqFuture<Boolean> removeChannel(String topic, String channel);  
	MqFuture<ChannelDetails> queryChannel(String topic, String channel); 
	
	void configAuth(Auth auth);
	
	public static class Auth{
		public String appId;
		public String token; 
	}
	
	public static class Topic {
		public String name;

		@Override
		public String toString() {
			return "Topic [name=" + name + "]";
		}  
	}
	
	public static class TopicDeclare {
		public String topic;
		public Boolean rpcFlag;
		public Map<String, String> properties = new HashMap<String, String>(); 
	}
	
	public static class TopicQuery {
		public String topic;
		public String appId;
		public Long createdTime; 
	}
	
	public static class TopicRemove {
		public String topic;
		public String appId;
		public Long createdTime; 
	}
	
	
	public static class ChannelDetails {
		public String topic;
		public String channel;
		@Override
		public String toString() {
			return "ChannelDetails [topic=" + topic + ", channel=" + channel + "]";
		} 
		
	}
	
	public static class ChannelDeclare {
		public String topic;
		public String channel;
		public String tag;
		
		public Boolean deleteOnExit;
		public Boolean exclusive;
		
		public Long consumeStartOffset;
		public Long consumeStartTime;  
	} 
	
	public static class ChannelQuery {
		public String topic;
		public String channel; 
	}
	
	public static class ChannelRemove {
		public String topic;
		public String channel; 
	}
}