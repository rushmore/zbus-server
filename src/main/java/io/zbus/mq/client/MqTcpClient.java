package io.zbus.mq.client;

import java.io.IOException;

import com.alibaba.fastjson.JSON;

import io.zbus.mq.api.Message;
import io.zbus.mq.api.MqClient;
import io.zbus.mq.api.MqFuture;
import io.zbus.mq.api.Protocol;
import io.zbus.mq.net.MessageClient;
import io.zbus.net.Future;
import io.zbus.net.IoDriver;
import io.zbus.net.Session;
import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory;

public class MqTcpClient extends MessageClient implements MqClient {
	private static final Logger log = LoggerFactory.getLogger(MqTcpClient.class); 
	private AckHandler produceAckHandler;
	private AckHandler consumeAckHandler;
	private StreamHandler streamHandler;
	private QuitHandler quitHandler;
	
	private Auth auth;
	
	public MqTcpClient(String address, IoDriver driver) {
		super(address, driver); 
	}

	private void fillCommonHeaders(Message message){
		if(auth != null){
			message.setAppId(auth.appId);
		}
		if(auth != null){
			message.setToken(auth.token);
		}
	}
	
	@Override
	public MqFuture<Topic> declareTopic(TopicDeclare ctrl) { 
		Message message = new Message();
		fillCommonHeaders(message);
		
		message.setCmd(Protocol.DECLARE_TOPIC);
		message.setJsonBody(JSON.toJSONBytes(ctrl)); 
		
		Future<Message> res = invoke(message);   
		
		DefaultMqFuture<Topic, Message> future = new DefaultMqFuture<Topic, Message>(res){
			@Override
			public Topic convert(Message result) {  
				
				Topic topic = JSON.parseObject(result.getBody(), Topic.class);
				return topic;
			}
		};
		return future;
	}
 

	@Override
	public void onProduceAck(AckHandler handler) {
		produceAckHandler = handler;
	}
	
	@Override
	public void onConsumeAck(AckHandler handler) {
		consumeAckHandler = handler;
	}

	@Override
	public void onStream(StreamHandler handler) {
		streamHandler = handler;
	}

	@Override
	public void onQuit(QuitHandler handler) {
		quitHandler = handler;
	}

	@Override
	public MqFuture<ProduceResult> produce(Message message) {
		message.setCmd(Protocol.PRODUCE);
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
		Message message = (Message)data; 
		String cmd = message.getCmd();
		
		if(Protocol.RESPONSE.equalsIgnoreCase(cmd)){
			boolean handled = handleInvokedMessage(data, sess);
			if(handled) return; 
		} 
		
		
		if(Protocol.PRODUCE_ACK.equalsIgnoreCase(cmd)){
			if(produceAckHandler != null){
				produceAckHandler.onAck(message);
				return;
			}
		}
		
		if(Protocol.CONSUME_ACK.equalsIgnoreCase(cmd)){
			if(consumeAckHandler != null){
				consumeAckHandler.onAck(message);
				return;
			}
		}
		
		if(Protocol.STREAM.equalsIgnoreCase(cmd)){
			if(streamHandler != null){
				streamHandler.onStream(message);
				return;
			}
		}
		
		if(Protocol.QUIT.equalsIgnoreCase(cmd)){
			if(quitHandler != null){
				quitHandler.onQuit(message);
				return;
			}
		}
		
		log.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!Drop unsupported command(%s),msg=%s", cmd, message);
	}

	@Override
	public void onData(DataHandler<Message> dataHandler) { 
		throw new UnsupportedOperationException("onData not support for MqTcpClient, you may need onStream");
	}

	@Override
	public void configAuth(Auth auth) {
		this.auth = auth;
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
}
