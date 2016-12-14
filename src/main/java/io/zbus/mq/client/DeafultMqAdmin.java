package io.zbus.mq.client;

import io.zbus.mq.api.ConsumeGroup;
import io.zbus.mq.api.ConsumeGroupCtrl;
import io.zbus.mq.api.Future;
import io.zbus.mq.api.Message;
import io.zbus.mq.api.MqAdmin;
import io.zbus.mq.api.Topic;
import io.zbus.mq.api.TopicCtrl;

public class DeafultMqAdmin implements MqAdmin { 
	
	@Override
	public Future<Topic> declareTopic(TopicCtrl topic) { 
		return null;
	}

	@Override
	public Future<Boolean> removeTopic(String topicName) { 
		return null;
	}

	@Override
	public Future<Topic> queryTopic(String topicName) {
		return null;
	}

	@Override
	public Future<ConsumeGroup> declareConsumeGroup(ConsumeGroupCtrl group) {
		return null;
	}

	@Override
	public Future<Boolean> removeConsumeGroup(String topicName, String groupName) {
		return null;
	}

	@Override
	public Future<ConsumeGroup> queryConsumeGroup(String topicName, String groupName) {
		return null;
	}

	@Override
	public void onAck(AckMessageHandler handler) {
		
	}

	@Override
	public void onData(DataMessageHandler handler) {
		
	}

	@Override
	public void onCtrl(CtrlMessageHandler handler) {
		
	}

	@Override
	public void route(String peerId, Message message) {
		
	}

	@Override
	public void start() {
		
	} 
}
