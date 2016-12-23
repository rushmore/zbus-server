package io.zbus.mq.client;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSON;

import io.zbus.mq.api.ConsumeGroup;
import io.zbus.mq.api.ConsumeHandler;
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

public class TcpMqClient extends MessageClient implements MqClient {
	private static final Logger log = LoggerFactory.getLogger(TcpMqClient.class);  
	
	private Auth auth;
	private Map<String, ConsumeContext> consumeContexts = new ConcurrentHashMap<String, ConsumeContext>();
	
	public TcpMqClient(String address, IoDriver driver) {
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
	
	private String key(String topic, String consumeGroup){
		String key = topic + "-->";
		if(consumeGroup != null) key += consumeGroup;
		return key;
	}

	@Override
	public MqFuture<ProduceResult> produce(Message message) {
		return null;
	}
	
	@Override
	public MqFuture<ConsumeResult> consume(ConsumeGroup consumeGroup, ConsumeHandler handler) { 
		String key = key(consumeGroup.getTopic(), consumeGroup.getConsumeGroup());
		ConsumeContext consumeContext = new ConsumeContext(consumeGroup, handler);
		consumeContexts.put(key, consumeContext); 
		
		return ready(consumeGroup);
	}
	
	
	@Override
	public MqFuture<ConsumeResult> ready(ConsumeGroup consumeGroup) {
		Message message = new Message();
		fillCommonHeaders(message);
		message.setCmd(Protocol.CONSUME); 
		message.setTopic(consumeGroup.getTopic());
		message.setConsumeGroup(consumeGroup.getConsumeGroup());
		message.setMaxInFlight(consumeGroup.getMaxInFlight()); 
		
		MqFuture<ConsumeResult> res = new DefaultMqFuture<ConsumeResult, Message>(invoke(message)){
			@Override
			public ConsumeResult convert(Message result) {
				ConsumeResult res = new ConsumeResult(); 
				return res;
			}
		};  
		return res;
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
	public void ack(String msgid, Long offset) { 
		
	}
	
	@Override
	public void sessionData(Object data, Session sess) throws IOException { 
		Message message = (Message)data; 
		String cmd = message.getCmd();
		
		if(Protocol.RESPONSE.equalsIgnoreCase(cmd)){
			boolean handled = handleInvokedMessage(data, sess);
			if(handled) return; 
		}  
		
		if(Protocol.STREAM.equalsIgnoreCase(cmd)){
			String topic = message.getTopic();
			String consumeGroup = message.getConsumeGroup();
			Integer window = message.getWindow();
			if(topic != null){
				String key = key(topic, consumeGroup);
				ConsumeContext ctx = consumeContexts.get(key);
				if(ctx != null){
					ctx.consumeHandler.onMessage(this, ctx.consumeGroup, message);
					if(window == null){//now window info, ack every time
						String msgid = message.getId();
						Long offset = message.getOffset();
						ack(msgid, offset);
					} else {
						if(window<25*ctx.consumeGroup.getMaxInFlight()/100){
							ready(ctx.consumeGroup);
						}
					} 
				}
			} 
		}  
		
		if(Protocol.QUIT.equalsIgnoreCase(cmd)){
			//TODO
		}   
		log.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!Drop,%s", message);
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
	public MqFuture<Topic> declareTopic(String topic, boolean rpcFlag) {
		TopicDeclare ctrl = new TopicDeclare();
		ctrl.topic = topic;
		ctrl.rpcFlag = rpcFlag; 
		return declareTopic(ctrl);
	}
	
	@Override
	public MqFuture<Topic> declareTopic(String topic) {
		return declareTopic(topic, false);
	}
 
	@Override
	public MqFuture<Boolean> removeTopic(TopicRemove ctrl) {
		return jsonInvoke(ctrl, Protocol.REMOVE_TOPIC, Boolean.class); 
	}
	
	@Override
	public MqFuture<Boolean> removeTopic(String topic) {
		TopicRemove ctrl = new TopicRemove();
		ctrl.topic = topic;
		return removeTopic(ctrl);
	}

	@Override
	public MqFuture<Topic> queryTopic(TopicQuery ctrl) {
		return jsonInvoke(ctrl, Protocol.QUERY_TOPIC, Topic.class);
	}
	
	@Override
	public MqFuture<Topic> queryTopic(String topic) {
		TopicQuery ctrl = new TopicQuery();
		ctrl.topic = topic;
		return queryTopic(ctrl);
	}

	@Override
	public MqFuture<ConsumeGroupDetails> declareConsumeGroup(ConsumeGroupDeclare ctrl) {
		return jsonInvoke(ctrl, Protocol.DECLARE_CONSUME_GROUP, ConsumeGroupDetails.class);
	}
	
	@Override
	public MqFuture<ConsumeGroupDetails> declareConsumeGroup(String topic, String consumeGroup) {
		ConsumeGroupDeclare ctrl = new ConsumeGroupDeclare();
		ctrl.topic = topic;
		ctrl.consumeGroup = consumeGroup;
		return declareConsumeGroup(ctrl);
	}

	@Override
	public MqFuture<Boolean> removeConsumeGroup(ConsumeGroupRemove ctrl) {
		return jsonInvoke(ctrl, Protocol.REMOVE_CONSUME_GROUP, Boolean.class); 
	}
	
	@Override
	public MqFuture<Boolean> removeConsumeGroup(String topic, String consumeGroup) {
		ConsumeGroupRemove ctrl = new ConsumeGroupRemove();
		ctrl.topic = topic;
		ctrl.consumeGroup = consumeGroup;
		return removeConsumeGroup(ctrl);
	}

	@Override
	public MqFuture<ConsumeGroupDetails> queryConsumeGroup(ConsumeGroupQuery ctrl) {
		return jsonInvoke(ctrl, Protocol.QUERY_CONSUME_GROUP, ConsumeGroupDetails.class);
	} 

	@Override
	public MqFuture<ConsumeGroupDetails> queryConsumeGroup(String topic, String consumeGroup) {
		ConsumeGroupQuery ctrl = new ConsumeGroupQuery();
		ctrl.topic = topic;
		ctrl.consumeGroup = consumeGroup;
		return queryConsumeGroup(ctrl);
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
	
	static class ConsumeContext{
		final ConsumeGroup consumeGroup;
		ConsumeHandler consumeHandler;
		public ConsumeContext(ConsumeGroup consumeGroup, ConsumeHandler consumeHandler) {
			this.consumeGroup = consumeGroup.clone(); 
			this.consumeHandler = consumeHandler;
		}
	} 
}
