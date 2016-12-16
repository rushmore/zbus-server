package io.zbus.mq.api;

import java.util.List;

public interface BrokerSelector {
	MqClient select(List<MqClient> clients, Object metaData);
}
