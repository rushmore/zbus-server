package io.zbus.mq.api;

import java.util.List;

public interface Broker {  
	List<MqClient> selectForProducer();  
	List<MqClient> selectForConsumer();   
	void addServerEventListener();
	void removeServerEventListener();
}
