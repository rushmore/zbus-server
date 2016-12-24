package io.zbus.mq.client;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSON;

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
	private Map<String, ChannelContext> channelCtxMap = new ConcurrentHashMap<String, ChannelContext>();
	
	public TcpMqClient(String address, IoDriver driver) {
		super(address, driver); 
	}

	@Override
	public MqFuture<ProduceResult> produce(Message message) {
		return null;
	}
	
	@Override
	public MqFuture<ConsumeResult> subscribe(String topic, String channel, int maxInFlight, ConsumeHandler handler) { 
		String key = key(topic, channel);
		ChannelContext ctx = new ChannelContext(topic, channel, handler, this);
		ctx.maxInFlight = maxInFlight; 
		
		channelCtxMap.put(key, ctx); 
		
		return ready(topic, channel, maxInFlight);
	}
	
	@Override
	public MqFuture<ConsumeResult> subscribe(String topic, int maxInFlight, ConsumeHandler handler) {
		return subscribe(topic, null, maxInFlight, handler);
	}
	
	@Override
	public MqFuture<ConsumeResult> subscribe(String topic, ConsumeHandler handler) {
		return subscribe(topic, null, 10, handler);
	}
	
	@Override
	public MqFuture<ConsumeResult> ready(String topic, String channel, int maxInFlight) {
		Message message = new Message();
		fillCommonHeaders(message);
		message.setCmd(Protocol.CONSUME); 
		message.setTopic(topic);
		message.setChannel(channel);
		message.setMaxInFlight(maxInFlight); 
		
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
	public MqFuture<ConsumeResult> unsubscribe(String topic, String channel) {
		return null;
	}

	@Override
	public MqFuture<ConsumeResult> unsubscribe(String topic) { 
		return null;
	}
	 
	
	@Override
	public void ack(String msgid, Long offset) { 
		
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
	public void sessionData(Object data, Session sess) throws IOException { 
		Message message = (Message)data; 
		String cmd = message.getCmd();
		
		if(Protocol.RESPONSE.equalsIgnoreCase(cmd)){
			boolean handled = handleInvokedMessage(data, sess);
			if(handled) return; 
		}  
		
		if(Protocol.STREAM.equalsIgnoreCase(cmd)){
			String topic = message.getTopic();
			String consumeGroup = message.getChannel();
			Integer window = message.getWindow();
			if(topic != null){
				String key = key(topic, consumeGroup);
				ChannelContext ctx = channelCtxMap.get(key);
				if(ctx != null){
					ctx.handler.onMessage(ctx, message);
					if(window == null){//now window info, ack every time
						String msgid = message.getId();
						Long offset = message.getOffset();
						ack(msgid, offset);
					} else {
						if(window<25*ctx.maxInFlight/100){
							ready(ctx.topic, ctx.channel, ctx.maxInFlight);
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
	public MqFuture<Topic> declareTopic(String topic, Long flag) {
		Message message = new Message();
		fillCommonHeaders(message);
		message.setCmd(Protocol.DECLARE_TOPIC);
		message.setTopic(topic); 
		message.setHeader("flag", ""+flag); 
		
		Future<Message> res = invoke(message);   
		
		DefaultMqFuture<Topic, Message> future = new DefaultMqFuture<Topic, Message>(res){
			@Override
			public Topic convert(Message result) {   
				return JSON.parseObject(result.getBodyAsString(), Topic.class);
			}
		};
		return future;
	}
	
	@Override
	public MqFuture<Topic> declareTopic(String topic) {
		return declareTopic(topic, null);
	}
	
	@Override
	public MqFuture<Topic> queryTopic(String topic) {
		return null;
	}
  
	@Override
	public MqFuture<Boolean> removeTopic(String topic) { 
		return null;
	} 

	@Override
	public MqFuture<Channel> declareChannel(ChannelDeclare ctrl) {
		Message message = new Message();
		fillCommonHeaders(message);
		message.setCmd(Protocol.DECLARE_CHANNEL);
		message.setTopic(ctrl.getTopic()); 
		message.setChannel(ctrl.getChannel()); 
		
		message.setHeader("tag", ctrl.getTag());
		message.setHeader("deleteOnExit", ctrl.getDeleteOnExit());
		message.setHeader("exclusive", ctrl.getExclusive());
		message.setHeader("consumeStartOffset", ctrl.getConsumeStartOffset());
		message.setHeader("consumeStartTime", ctrl.getConsumeStartTime());
		
		Future<Message> res = invoke(message);   
		
		DefaultMqFuture<Channel, Message> future = new DefaultMqFuture<Channel, Message>(res){
			@Override
			public Channel convert(Message result) {   
				return JSON.parseObject(result.getBodyAsString(), Channel.class);
			}
		};
		return future;
	}
	
	@Override
	public MqFuture<Channel> declareChannel(String topic, String channel) {
		ChannelDeclare ctrl = new ChannelDeclare();
		ctrl.setTopic(topic);
		ctrl.setChannel(channel);
		return declareChannel(ctrl);
	}
	  
	@Override
	public MqFuture<Channel> queryChannel(String topic, String channel) {
		return null;
	}
	
	@Override
	public MqFuture<Boolean> removeChannel(String topic, String channel) {
		return null;
	}  
	
}
