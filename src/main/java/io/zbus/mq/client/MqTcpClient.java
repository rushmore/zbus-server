package io.zbus.mq.client;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.fastjson.JSON;

import io.zbus.mq.api.ConsumerHandler;
import io.zbus.mq.api.Message;
import io.zbus.mq.api.MqClient;
import io.zbus.mq.api.MqFuture;
import io.zbus.mq.api.Protocol;
import io.zbus.mq.net.MessageClient;
import io.zbus.net.Future;
import io.zbus.net.IoDriver;
import io.zbus.net.Session;

public class MqTcpClient extends MessageClient implements MqClient {
	 //private static final Logger log = LoggerFactory.getLogger(MqTcpClient.class);  
	
	private Auth auth;
	private Map<String, ConsumeGroup> consumeGroups = new ConcurrentHashMap<String, ConsumeGroup>();
	
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
	public MqFuture<ProduceResult> produce(Message message) {
		message.setCmd(Protocol.PRODUCE);
		if(message.getAck() == false){
			send(message); 
		} else {
			//Future<Message> res = invoke(message);
		} 
		return null;
	} 
	
	private String key(String topic, String consumeGroup){
		String key = topic + "-->";
		if(consumeGroup != null) key += consumeGroup;
		return key;
	}

	@Override
	public MqFuture<ConsumeResult> consume(ConsumerHandler handler) {
		String key = key(handler.topic(), handler.consumeGroup());
		ConsumeGroup group = new ConsumeGroup();
		group.topic = handler.topic();
		group.consumeGroup = handler.consumeGroup();
		group.handler = handler;
		group.maxInFlightMessage = handler.maxInFlightMessage();
		
		consumeGroups.put(key, group); 
		 
		return null;
	}
 

	@Override
	public MqFuture<ConsumeResult> cancelConsume(String topic, String consumeGroup) {
		return null;
	}

	@Override
	public MqFuture<ConsumeResult> cancelConsume(String topic) { 
		return null;
	}
	
	@Override
	public void ack(Message message) { 
		
	}
	
	@Override
	public void sessionData(Object data, Session sess) throws IOException { 
		Message message = (Message)data; 
		String cmd = message.getCmd();
		
		if(Protocol.RESPONSE.equalsIgnoreCase(cmd)){
			boolean handled = handleInvokedMessage(data, sess);
			if(handled) return; 
		}  
		
		String topic = message.getTopic();
		String consumeGroup = message.getConsumeGroup();
		if(topic != null){
			String key = key(topic, consumeGroup);
			ConsumeGroup group = consumeGroups.get(key);
			if(group != null){
				
			}
		} 
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
	public MqFuture<Topic> declareTopic(TopicDeclare ctrl) { 
		return jsonInvoke(ctrl, Protocol.DECLARE_TOPIC, Topic.class); 
	}
 
	@Override
	public MqFuture<Boolean> removeTopic(TopicRemove ctrl) {
		return jsonInvoke(ctrl, Protocol.REMOVE_TOPIC, Boolean.class); 
	}

	@Override
	public MqFuture<Topic> queryTopic(TopicQuery ctrl) {
		return jsonInvoke(ctrl, Protocol.QUERY_TOPIC, Topic.class);
	}

	@Override
	public MqFuture<Channel> declareChannel(ChannelDeclare ctrl) {
		return jsonInvoke(ctrl, Protocol.DECLARE_CHANNEL, Channel.class);
	}

	@Override
	public MqFuture<Boolean> removeChannel(ChannelRemove ctrl) {
		return jsonInvoke(ctrl, Protocol.REMOVE_CHANNEL, Boolean.class); 
	}

	@Override
	public MqFuture<Channel> queryChannel(ChannelQuery ctrl) {
		return jsonInvoke(ctrl, Protocol.QUERY_CHANNEL, Channel.class);
	}
	
	private <V> MqFuture<V> jsonInvoke(Object ctrl, String cmd, final Class<V> clazz){
		Message message = new Message();
		fillCommonHeaders(message);
		
		message.setCmd(cmd);
		message.setJsonBody(JSON.toJSONBytes(ctrl)); 
		
		Future<Message> res = invoke(message);   
		
		DefaultMqFuture<V, Message> future = new DefaultMqFuture<V, Message>(res){
			@Override
			public V convert(Message result) {   
				return JSON.parseObject(result.getBody(), clazz); 
			}
		};
		return future;
	} 
	
	static class ConsumeGroup{
		int maxInFlightMessage = 20;
		String topic;
		String consumeGroup;
		final AtomicInteger window = new AtomicInteger(maxInFlightMessage);
		ConsumerHandler handler; 
	}
}
