package io.zbus.mq.api;

/**
 * 
 * Consumer within a same ConsumeGroup, consumes the message in load-balance way.
 * 
 * 
 * 
 * @author Rushmore
 *
 */
public interface Consumer extends MqAdmin{ 
	
	Message take(int timeout);
	
	void subscribe(String consumeGroup);
	
	void unsubscribe(String consumeGroup);
	
	void applyFilter(String tag);
	
	void removeFilter(String tag);    
}
