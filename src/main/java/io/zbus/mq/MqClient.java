package io.zbus.mq;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.JsonKit;
import io.zbus.transport.Client;
import io.zbus.transport.DataHandler;
import io.zbus.transport.Message; 

public class MqClient extends Client{ 
	private static final Logger logger = LoggerFactory.getLogger(MqClient.class);  
	
	private List<MqHandler> handlers = new ArrayList<>();
	
	public static class MqHandler{
		public String mq;
		public String channel;
		public Integer window;
		public DataHandler<Message> handler;
	}
	
	public MqClient(String address) {  
		super(address);
		onMessage(msg->{
			handleMessage(msg);
		});
	}  
	
	public MqClient(MqServer server) {
		super(server.getServerAdaptor()); 
		onMessage(msg->{
			handleMessage(msg);
		});
	}
	
	private void handleMessage(Message response) throws Exception {
		boolean handled = handleInvokeResponse(response);
		if(handled) return; 
		
		//Subscribed message pushing 
		String mq = (String)response.getHeader(Protocol.MQ);
		String channel = (String)response.getHeader(Protocol.CHANNEL);
		if(mq == null || channel == null) {
			logger.warn("MQ/Channel both required in reponse: " + JsonKit.toJSONString(response));
			return;
		} 
		MqHandler mqHandler = getHandler(mq, channel); 
		if(mqHandler == null) {
			logger.warn(String.format("Missing handler for mq=%s,channel=%s",mq, channel));
			return;
		} 
		try {
			mqHandler.handler.handle(response); 
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} 
		//increase window if required
		Integer window = response.getHeaderInt(Protocol.WINDOW);
		if(window != null) { 
			if(window <= mqHandler.window/2) {
				try {
					Message sub = new Message();
					sub.setHeader(Protocol.CMD, Protocol.SUB);
					sub.setHeader(Protocol.MQ, mq);
					sub.setHeader(Protocol.CHANNEL, channel);
					sub.setHeader(Protocol.WINDOW, mqHandler.window);
					sub.setHeader(Protocol.ACK, false);
					
					this.sendMessage(sub); 
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		} 
	}
	 
	public synchronized void heartbeat(long interval, TimeUnit timeUnit) {
		heartbeat(interval, timeUnit, ()->{
			Message msg = new Message();
			msg.setHeader(Protocol.CMD, Protocol.PING);
			return msg;
		});
	}  
	
	private MqHandler getHandler(String mq, String channel) {
		for(MqHandler handler : handlers) {
			if(mq.equals(handler.mq) && channel.equals(handler.channel)) {
				return handler;
			} 
		}
		return null;
	}
	
	public void addMqHandler(String mq, String channel, DataHandler<Message> dataHandler) {
		addMqHandler(mq, channel, 1, dataHandler);
	}  

	public void addMqHandler(String mq, String channel, Integer window, DataHandler<Message> dataHandler) {
		for(MqHandler handler : handlers) {
			if(mq.equals(handler.mq) && channel.equals(handler.channel)) {
				handler.window = window;
				handler.handler = dataHandler;
				return;
			}  
		}
		
		MqHandler mqHandler = new MqHandler();
		mqHandler.mq = mq;
		mqHandler.channel = channel;
		mqHandler.window = window;
		mqHandler.handler = dataHandler;
		
		handlers.add(mqHandler); 
	}  
}
