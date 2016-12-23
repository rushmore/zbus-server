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
    
	MqFuture<ConsumeGroupDetails> declareConsumeGroup(ConsumeGroupDeclare ctrl); 
	MqFuture<Boolean> removeConsumeGroup(ConsumeGroupRemove ctrl);  
	MqFuture<ConsumeGroupDetails> queryConsumeGroup(ConsumeGroupQuery ctrl);    
	MqFuture<ConsumeGroupDetails> declareConsumeGroup(String topic, String consumeGroup); 
	MqFuture<Boolean> removeConsumeGroup(String topic, String consumeGroup);  
	MqFuture<ConsumeGroupDetails> queryConsumeGroup(String topic, String consumeGroup); 
	
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
	
	
	public static class ConsumeGroupDetails {
		public String topic;
		public String consumeGroup;
		@Override
		public String toString() {
			return "ConsumeGroupDetails [topic=" + topic + ", channel=" + consumeGroup + "]";
		} 
		
	}
	
	public static class ConsumeGroupDeclare {
		public String topic;
		public String consumeGroup;
		public String tag;
		
		public Boolean deleteOnExit;
		public Boolean exclusive;
		
		public Long consumeStartOffset;
		public Long consumeStartTime;  
	} 
	
	public static class ConsumeGroupQuery {
		public String topic;
		public String consumeGroup; 
	}
	
	public static class ConsumeGroupRemove {
		public String topic;
		public String consumeGroup; 
	}
}