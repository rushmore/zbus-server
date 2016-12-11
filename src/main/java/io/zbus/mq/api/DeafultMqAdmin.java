package io.zbus.mq.api;

public class DeafultMqAdmin implements MqAdmin { 
	
	@Override
	public MqFuture<Topic> declareTopic(TopicCtrl topic) { 
		return null;
	}

	@Override
	public MqFuture<Boolean> removeTopic(String topicName) { 
		return null;
	}

	@Override
	public MqFuture<Topic> queryTopic(String topicName) {
		return null;
	}

	@Override
	public MqFuture<ConsumeGroup> declareConsumeGroup(ConsumeGroupCtrl group) {
		return null;
	}

	@Override
	public MqFuture<Boolean> removeConsumeGroup(String topicName, String groupName) {
		return null;
	}

	@Override
	public MqFuture<ConsumeGroup> queryConsumeGroup(String topicName, String groupName) {
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
