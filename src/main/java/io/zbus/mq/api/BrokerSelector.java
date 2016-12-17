package io.zbus.mq.api;

import java.util.List;

public interface BrokerSelector {
	List<MqClient> selectForAdmin(List<MqClient> clients, Object metaData);
	List<MqClient> selectForProducer(List<MqClient> clients, Object metaData);
	List<MqClient> selectForConsumer(List<MqClient> clients, Object metaData);
}
