package io.zbus.mq.api;

import java.io.Closeable;

public interface MqAdmin extends Closeable{   
	
	Future<Topic> declareTopic(String topic, Long flag); 
	Future<Topic> declareTopic(String topic); 
	Future<Topic> queryTopic(String topic); 
	Future<Boolean> removeTopic(String topic); 
    
	Future<Channel> declareChannel(ChannelDeclare ctrl);  
	Future<Channel> declareChannel(String topic, String channel);  
	Future<Channel> queryChannel(String topic, String channel);     
	Future<Boolean> removeChannel(String topic, String channel);   
	
	void configAuth(Auth auth);
	
	public static class Auth{
		public String appId;
		public String token; 
	}
	
	public static class Topic {
		private String name;
		private long flag;
		
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public long getFlag() {
			return flag;
		}

		public void setFlag(long flag) {
			this.flag = flag;
		}

		@Override
		public String toString() {
			return "Topic [name=" + name + ", flag=" + flag + "]";
		}  
		
	} 
	
	public static class Channel {
		private String topic;
		private String channel; 
		
		public String getTopic() {
			return topic;
		}  
		public void setTopic(String topic) {
			this.topic = topic;
		}  
		public String getChannel() {
			return channel;
		} 
		public void setChannel(String channel) {
			this.channel = channel;
		} 
		@Override
		public String toString() {
			return "Channel [topic=" + topic + ", channel=" + channel + "]";
		}  
	}
	
	public static class ChannelDeclare {
		private String topic;
		private String channel;
		private String tag;
		
		private Boolean deleteOnExit;
		private Boolean exclusive;
		
		private Long consumeStartOffset;
		private Long consumeStartTime;
		
		public String getTopic() {
			return topic;
		}
		public void setTopic(String topic) {
			this.topic = topic;
		}
		public String getChannel() {
			return channel;
		}
		public void setChannel(String channel) {
			this.channel = channel;
		}
		public String getTag() {
			return tag;
		}
		public void setTag(String tag) {
			this.tag = tag;
		}
		public Boolean getDeleteOnExit() {
			return deleteOnExit;
		}
		public void setDeleteOnExit(Boolean deleteOnExit) {
			this.deleteOnExit = deleteOnExit;
		}
		public Boolean getExclusive() {
			return exclusive;
		}
		public void setExclusive(Boolean exclusive) {
			this.exclusive = exclusive;
		}
		public Long getConsumeStartOffset() {
			return consumeStartOffset;
		}
		public void setConsumeStartOffset(Long consumeStartOffset) {
			this.consumeStartOffset = consumeStartOffset;
		}
		public Long getConsumeStartTime() {
			return consumeStartTime;
		}
		public void setConsumeStartTime(Long consumeStartTime) {
			this.consumeStartTime = consumeStartTime;
		}   
	}  
}