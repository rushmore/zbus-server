package io.zbus.mq.api;

import io.zbus.mq.api.MqClient.ProduceResult;

public interface Producer extends MqAdmin { 
	MqFuture<ProduceResult> publish(Message message);  

}
