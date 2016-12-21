package io.zbus.mq.client;

import io.zbus.mq.api.MqAdmin;
import io.zbus.mq.api.MqFuture;

public class DefaultMqAdmin implements MqAdmin {  
	 
	
	@Override
	public MqFuture<Topic> declareTopic(TopicDeclare ctrl) {
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
}
