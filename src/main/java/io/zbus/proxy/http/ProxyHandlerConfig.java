package io.zbus.proxy.http;

import io.zbus.mq.Broker;

public class ProxyHandlerConfig {
	public String topic; 
	public Broker broker; 
	public String token;
	public int consumerCount;
	public int consumeTimeout;

	public String targetServer; //host:ip
	public String targetUrl;    //after host:ip
	public int targetHeartbeat;  
	public int targetClientCount;
	public boolean targetMessageIdentifiable = false;
	 
	public MessageFilter sendFilter;
	public MessageFilter recvFilter; 
}