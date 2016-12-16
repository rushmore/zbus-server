package io.zbus.mq.api;

import java.util.List;

public interface Broker { 
	//how to select MqClient to work
	MqClient selectOne();
	List<MqClient> selectAll();
	void returnOne(MqClient client);
	void returnAll(List<MqClient> clients);
	
	MqClient filter(List<MqClient> clients, Object metaData);
}
