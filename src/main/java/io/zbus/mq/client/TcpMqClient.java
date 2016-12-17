package io.zbus.mq.client;

import java.io.IOException;

import io.zbus.mq.api.Channel;
import io.zbus.mq.api.ChannelCtrl;
import io.zbus.mq.api.Message;
import io.zbus.mq.api.MqClient;
import io.zbus.mq.api.MqFuture;
import io.zbus.mq.api.Protocol;
import io.zbus.mq.api.Topic;
import io.zbus.mq.api.TopicCtrl;
import io.zbus.mq.net.MessageClient;
import io.zbus.net.IoDriver;
import io.zbus.net.Session;

public class TcpMqClient extends MessageClient implements MqClient {
	private AckMessageHandler ackMessageHandler;
	private DataMessageHandler dataMessageHandler;
	private CtrlMessageHandler ctrlMessageHandler;
	
	public TcpMqClient(String address, IoDriver driver) {
		super(address, driver); 
	}

	@Override
	public MqFuture<Topic> declareTopic(TopicCtrl ctrl) {
		return null;
	}

	@Override
	public MqFuture<Boolean> removeTopic(String topic) {
		return null;
	}

	@Override
	public MqFuture<Topic> queryTopic(String topic) {
		return null;
	}

	@Override
	public MqFuture<Channel> declareChannel(ChannelCtrl ctrl) {
		return null;
	}

	@Override
	public MqFuture<Boolean> removeChannel(String topic, String channel) {
		return null;
	}

	@Override
	public MqFuture<Channel> queryChannel(String topic, String channel) {
		return null;
	}

	@Override
	public void onAck(AckMessageHandler handler) {
		ackMessageHandler = handler;
	}

	@Override
	public void onData(DataMessageHandler handler) {
		dataMessageHandler = handler;
	}

	@Override
	public void onCtrl(CtrlMessageHandler handler) {
		ctrlMessageHandler = handler;
	}

	@Override
	public MqFuture<Message> produce(Message message) {
		message.setCmd("produce");
		send(message); 
		return null;
	}

	@Override
	public MqFuture<Void> consume(ChannelCtrl ctrl) {
		return null;
	}

	@Override
	public Message take(int timeout) {
		return null;
	}
	
	@Override
	public void sessionData(Object data, Session sess) throws IOException { 
		Message message = (Message)data; 
		
		if(Protocol.ACK.equalsIgnoreCase(message.getCmd())){
			if(ackMessageHandler != null){
				ackMessageHandler.onAck(message);
				return;
			}
		}
		
		if(Protocol.DATA.equalsIgnoreCase(message.getCmd())){
			if(dataMessageHandler != null){
				dataMessageHandler.onData(message);
				return;
			}
		}
		
		if(Protocol.CTRL.equalsIgnoreCase(message.getCmd())){
			if(ctrlMessageHandler != null){
				ctrlMessageHandler.onCtrl(message.getSubCmd(), message);
				return;
			}
		}
		
		super.sessionData(data, sess);
	}

}
