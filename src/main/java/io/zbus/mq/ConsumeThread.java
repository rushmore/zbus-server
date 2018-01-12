package io.zbus.mq;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.TimeUnit;

import io.zbus.kit.ThreadKit.ManualResetEvent;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Protocol.ConsumeGroupInfo;

public class ConsumeThread implements Closeable{
	private static final Logger log = LoggerFactory.getLogger(ConsumeThread.class);  
	protected final MqClient client; 
	
	protected Topic topic;
	protected ConsumeGroup consumeGroup; 
	protected ConsumeCtrl consumeCtrl; 
	protected boolean declareOnMissing = true;
	protected String token; 
	protected MessageHandler messageHandler;
	
	protected ManualResetEvent active = new ManualResetEvent(true); 
	
	protected RunningThread consumeThread;
	 
	public ConsumeThread(MqClient client, Topic topic, ConsumeGroup group, ConsumeCtrl consumeCtrl){
		this.client = client;
		this.topic = topic;
		this.consumeGroup = group;
		this.consumeCtrl = consumeCtrl;
	}
	 
	 
	public synchronized void start() {
		start(false);
	}
	
	public synchronized void start(boolean pauseOnStart) {
		if(this.topic == null){
			throw new IllegalStateException("Missing topic");
		}
		if(this.messageHandler == null){
			throw new IllegalStateException("Missing consumeHandler");
		}
		if(pauseOnStart){
			active.reset();
		} 
		this.client.setToken(token);
		this.client.setInvokeTimeout(consumeCtrl.getConsumeTimeout());
		
		if(declareOnMissing){
			try { 
				ConsumeGroupInfo info = this.client.declareGroup(topic, consumeGroup); 
				consumeCtrl.setConsumeGroup(info.groupName); //update groupName
			} catch (IOException e) { 
				log.error(e.getMessage(), e);
			} catch (InterruptedException e) { 
				log.error(e.getMessage(), e);
			}
		} 
		
		consumeThread = new RunningThread();
		consumeThread.start();
	}
	
	public void pause(){
		try {
			client.unconsume(topic.getName(), consumeCtrl.getConsumeGroup()); //stop consuming in server side
			consumeThread.running = false;
			consumeThread.interrupt();
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}  
		
		active.reset();
	}
	
	public void resume(){  
		active.set();
		if(consumeThread == null || !consumeThread.running) {
			consumeThread = new RunningThread();
			consumeThread.start();
		}
	}
	
	public Message take() throws IOException, InterruptedException {  
		Message res = null;
		try {  
			res = client.consume(consumeCtrl); 
			if (res == null) return res; 
			Integer status = res.getStatus();
			if (status == 404) { 
				if(!declareOnMissing){
					throw new MqException(res.getBodyString());
				}
				ConsumeGroupInfo info = client.declareGroup(topic, consumeGroup);
				consumeCtrl.setConsumeGroup(info.groupName); //update groupName
				return take();
			}
			
			if (status == 200) { 
				
				String method = res.getOriginMethod();
				if(method != null){
					res.setMethod(method);
					res.removeHeader(Protocol.ORIGIN_METHOD);
				}
				
				String originUrl = res.getOriginUrl();
				if(originUrl != null){ 
					res.removeHeader(Protocol.ORIGIN_URL);
					res.setUrl(originUrl);   
					res.setStatus(null);
					
					return res;
				}
				
				Integer originStatus = res.getOriginStatus();
				if(originStatus != null){ 
					res.removeHeader(Protocol.ORIGIN_STATUS);  
					res.setStatus(originStatus);
					return res;
				}
				
				res.setStatus(null);//default to request type
				return res;
			}
			
			throw new MqException(res.getBodyString());
		} catch (ClosedByInterruptException e) {
			throw new InterruptedException(e.getMessage());
		}   
	} 
	
	
	class RunningThread extends Thread {
		volatile boolean running = true; 
		
		public void run() { 
			while (running) {
				try { 
					final Message msg;
					try {
						while(running){
							active.await(1000, TimeUnit.MILLISECONDS); 
							if(active.isSignalled()) break;
						}
						if(!running) break; 
						msg = take();
						if(msg == null) continue; 
						if(!running) break;
						
						if(messageHandler == null){
							throw new IllegalStateException("Missing ConsumeHandler");
						}
						
						try{
							messageHandler.handle(msg, client);
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						} 
						
					} catch (InterruptedException e) {
						client.close(); 
						break;
					}  
					
				} catch (IOException e) {  
					log.error(e.getMessage(), e);  
				}
			}
		}
	}
	  

	@Override
	public void close() throws IOException {
		consumeThread.interrupt();  
	}  
	
	public void setMessageHandler(MessageHandler messageHandler) {
		this.messageHandler = messageHandler;
	} 
	 
	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}  

	public MessageHandler getMessageHandler() {
		return messageHandler;
	}  
	
	public boolean isDeclareOnMissing() {
		return declareOnMissing;
	} 

	public void setDeclareOnMissing(boolean declareOnMissing) {
		this.declareOnMissing = declareOnMissing;
	}


	public MqClient getClient() {
		return client;
	} 
}