package io.zbus.mq.client;

import java.io.IOException;

import io.zbus.mq.api.Message;
import io.zbus.mq.api.MqClient;
import io.zbus.mq.api.Protocol;
import io.zbus.mq.net.MessageClient;
import io.zbus.net.IoDriver;
import io.zbus.net.Session;
import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory;

public class MqTcpClient extends MessageClient implements MqClient {
	private static final Logger log = LoggerFactory.getLogger(MqTcpClient.class); 
	private AckHandler ackHandler;
	private DataHandler dataHandler;
	private CtrlHandler ctrlHandler;
	
	public MqTcpClient(String address, IoDriver driver) {
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
	public void onAck(AckHandler handler) {
		ackHandler = handler;
	}

	@Override
	public void onData(DataHandler handler) {
		dataHandler = handler;
	}

	@Override
	public void onCtrl(CtrlHandler handler) {
		ctrlHandler = handler;
	}

	@Override
	public MqFuture<ProduceResult> produce(Message message) {
		message.setCmd("produce");
		if(message.getAck() == false){
			send(message); 
		} else {
			//Future<Message> res = invoke(message);
		} 
		return null;
	}

	@Override
	public MqFuture<ConsumeResult> consume(ConsumeCtrl ctrl) {
		return null;
	}

	@Override
	public Message take(int timeout) {
		return null;
	}
	
	@Override
	public void sessionData(Object data, Session sess) throws IOException { 
		boolean handled = handleInvokedMessage(data, sess);
		if(handled) return;
		
		Message message = (Message)data; 
		
		if(Protocol.ACK.equalsIgnoreCase(message.getCmd())){
			if(ackHandler != null){
				ackHandler.onAck(message);
				return;
			}
		}
		
		if(Protocol.DATA.equalsIgnoreCase(message.getCmd())){
			if(dataHandler != null){
				dataHandler.onData(message);
				return;
			}
		}
		
		if(Protocol.CTRL.equalsIgnoreCase(message.getCmd())){
			if(ctrlHandler != null){
				ctrlHandler.onCtrl(message.getSubCmd(), message);
				return;
			}
		}
		
		log.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!Drop,%s", message);
	}

	@Override
	public void onMessage(MsgHandler<Message> msgHandler) { 
		throw new UnsupportedOperationException("onMessage not support for TcpMqClient");
	}
}
