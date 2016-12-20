package io.zbus.mq.api;

import java.util.HashMap;
import java.util.Map;

import io.zbus.mq.api.MqClient.MqFuture;

public interface MqAdmin{   
	
	MqFuture<Topic> declareTopic(TopicCtrl ctrl); 
	MqFuture<Boolean> removeTopic(String topic);  
	MqFuture<Topic> queryTopic(String topic); 
    
	MqFuture<Channel> declareChannel(ChannelCtrl ctrl); 
	MqFuture<Boolean> removeChannel(String topic, String channel);  
	MqFuture<Channel> queryChannel(String topic, String channel);   
	
	void configAuth(Auth auth);
	
	public static class Auth{
		public String appId;
		public String token;
		
		public Auth(){ }
		
		public Auth(String appId, String token){
			this.appId = appId;
			this.token = token;
		}
		
		public String getAppId() {
			return appId;
		}
		public void setAppId(String appId) {
			this.appId = appId;
		}
		public String getToken() {
			return token;
		}
		public void setToken(String token) {
			this.token = token;
		} 
	}
	
	public static class Topic {
		public String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return "Topic [name=" + name + "]";
		}  
	}
	
	public static class TopicCtrl {
		public String topic;
		public Map<String, String> properties = new HashMap<String, String>();

		public String getTopic() {
			return topic;
		}

		public void setTopic(String topic) {
			this.topic = topic;
		}

		public Map<String, String> getProperties() {
			return properties;
		}

		public void setProperties(Map<String, String> properties) {
			this.properties = properties;
		}  
		
		public void addProperty(String key, String value){
			properties.put(key, value);
		}
		
		public void removeProperty(String key){
			properties.remove(key);
		}
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