package io.zbus.mq.api;

import io.zbus.mq.api.MqClient.ConsumeCtrl;

/**
 * Abstraction of MqClient from the Consumer point of view. 
 * 
 * Features:
 * 1) Dynamic connection to multiple MqServer
 * 2) Control all MqClient from different MqServer in the same manner.
 * 
 * From the high level view, dynamic number of MqServer is a single virtual MqServer.
 * Any server failure should be affect the path from producer to end consumer.
 * 
 * @author Rushmore
 *
 */
public interface Consumer extends MqAdmin {    
	void start(ConsumeCtrl ctrl);   
	
	void pause(String topic, String channel);  
	
	void onData();
	void onCtrl();
	void onAck(); 
}
