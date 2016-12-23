package io.zbus.mq.api;

import java.util.concurrent.atomic.AtomicInteger;

public class ConsumeGroup {
	public int maxInFlightMessage = 100;
	public String topic;
	public String consumeGroup;  
	public AtomicInteger window = new AtomicInteger(maxInFlightMessage);
	public Boolean ack; 
	public ConsumeHandler handler;
}