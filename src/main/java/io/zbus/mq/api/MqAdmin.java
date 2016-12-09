package io.zbus.mq.api;

import java.util.Map;

public class MqAdmin{     
	protected final Broker broker; 
	
	protected String topic;
	protected String appId;
	protected String token;
	
	protected MessageHandler dataHandler;
	protected AckMessageHandler ackHandler;
	protected CtrlMessageHandler ctrlHandler;
	
	public MqAdmin(Broker broker){  
		this.broker = broker; 
	}  
	
	public void declareTopic(String topic, Map<String, Object> properties){
		
	}
	
	public void removeTopic(String topic){
		
	}
	
	public void queryTopic(String topic){
		
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Broker getBroker() {
		return broker;
	}
	
	
	public void onAck(AckMessageHandler handler){
		ackHandler = handler;
	}
	
	public void onData(MessageHandler handler){
		dataHandler = handler;
	} 
	
	public void onCtrl(CtrlMessageHandler handler){
		ctrlHandler = handler;
	} 
	
	public static interface AckMessageHandler {
		void onAck(String msgId, Message message);
	}
	public static interface CtrlMessageHandler {
		void onCtrl(String cmd, Message message);
	}
}