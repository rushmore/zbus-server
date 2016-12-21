package io.zbus.mq.api;

import java.util.concurrent.atomic.AtomicInteger;

import io.zbus.mq.api.MqClient.ProduceResult;
import io.zbus.mq.net.MessageClient;

public class Channel {
	private MessageClient client;
	private String topic;
	private String channel;
	private AtomicInteger window = new AtomicInteger(100);
	
	public MqFuture<ProduceResult> publish(byte[] body){
		return null;
	}

	
	
}
