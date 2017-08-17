
package io.zbus.mq;

import java.io.IOException;

import io.zbus.mq.Protocol.ConsumeGroupInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.transport.Session;

public interface MessageQueue { 

	void produce(Message message) throws IOException; 
	
	Message consume(String consumeGroup, Integer window) throws IOException;  
	
	void consume(Message message, Session session) throws IOException;  
	
	ConsumeGroupInfo declareGroup(ConsumeGroup consumeGroup) throws Exception;
	
	void removeGroup(String groupName) throws IOException; 
	
	void removeTopic() throws IOException; 
	
	int consumerCount(String consumeGroup); 
	
	void cleanSession(Session sess);  
	
	String getTopic();
	
	TopicInfo getInfo();
	
	long getUpdateTime();  
	
	String getCreator();

	void setCreator(String value); 
	
	int getMask();

	void setMask(int value);  
}