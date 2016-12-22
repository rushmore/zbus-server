package io.zbus.mq.client;

import java.io.IOException;

import io.zbus.mq.api.ConsumerHandler;
import io.zbus.mq.api.Message;
import io.zbus.mq.api.MqClient;
import io.zbus.mq.api.MqFuture;

public class JvmMqClient implements MqClient {

	@Override
	public MqFuture<ProduceResult> produce(Message message) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MqFuture<Topic> declareTopic(TopicDeclare ctrl) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MqFuture<Boolean> removeTopic(TopicRemove ctrl) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MqFuture<Topic> queryTopic(TopicQuery ctrl) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MqFuture<Channel> declareChannel(ChannelDeclare ctrl) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MqFuture<Boolean> removeChannel(ChannelRemove ctrl) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MqFuture<Channel> queryChannel(ChannelQuery ctrl) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configAuth(Auth auth) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public MqFuture<ConsumeResult> consume(ConsumerHandler handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MqFuture<ConsumeResult> cancelConsume(String topic, String consumeGroup) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MqFuture<ConsumeResult> cancelConsume(String topic) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void ack(Message message) {
		// TODO Auto-generated method stub
		
	}
	 
}
