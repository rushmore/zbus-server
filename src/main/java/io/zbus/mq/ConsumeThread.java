package io.zbus.mq;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.ExecutorService;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;

public class ConsumeThread extends Thread implements Closeable{
	private static final Logger log = LoggerFactory.getLogger(ConsumeThread.class);  
	protected final MqClient client;
	
	protected String topic; 
	protected String token;
	protected ConsumeGroup consumeGroup;
	protected int consumeTimeout = 10000;
	protected Integer consumeWindow;
	
	protected ExecutorService consumeRunner;
	protected MessageHandler consumeHandler;
	 
	 
	public ConsumeThread(MqClient client, String topic, ConsumeGroup group){
		this.client = client;
		this.topic = topic;
		this.consumeGroup = group;
	}
	
	public ConsumeThread(MqClient client, String topic){
		this(client, topic, null);
	}
	
	public ConsumeThread(MqClient client){
		this(client, null);
	}
	
	@Override
	public synchronized void start() {
		if(this.topic == null){
			throw new IllegalStateException("Missing topic");
		}
		if(this.consumeHandler == null){
			throw new IllegalStateException("Missing consumeHandler");
		}
		if(this.consumeGroup == null){
			this.consumeGroup = new ConsumeGroup();
			consumeGroup.setGroupName(this.topic);
		}  
		this.client.setToken(token);
		this.client.setInvokeTimeout(consumeTimeout);
		
		super.start();
	}
	
	public Message take() throws IOException, InterruptedException {  
		Message res = null;
		try {  
			res = client.consume(topic, this.consumeGroup.getGroupName(), this.getConsumeWindow()); 
			if (res == null) return res; 
			Integer status = res.getStatus();
			if (status == 404) {
				client.declareGroup(topic, consumeGroup); 
				return take();
			}
			
			if (status == 200) {
				return res;
			}
			
			throw new MqException(res.getBodyString());
		} catch (ClosedByInterruptException e) {
			throw new InterruptedException(e.getMessage());
		}   
	} 
	
	@Override
	public void run() {  
		while (true) {
			try { 
				final Message msg;
				try {
					msg = take();
					if(msg == null) continue;
					
					if(consumeHandler == null){
						throw new IllegalStateException("Missing ConsumeHandler");
					}
					
					if(consumeRunner == null){
						try{
							consumeHandler.handle(msg, client);
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
					} else {
						consumeRunner.submit(new Runnable() { 
							@Override
							public void run() {
								try{
									consumeHandler.handle(msg, client);
								} catch (Exception e) {
									log.error(e.getMessage(), e);
								}
							}
						});
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

	@Override
	public void close() throws IOException {
		interrupt();  
	} 

	public ExecutorService getConsumeRunner() {
		return consumeRunner;
	}

	public void setConsumeRunner(ExecutorService consumeRunner) {
		this.consumeRunner = consumeRunner;
	}

	public void setConsumeTimeout(int consumeTimeout) {
		this.consumeTimeout = consumeTimeout;
	}

	public void setConsumeHandler(MessageHandler consumeHandler) {
		this.consumeHandler = consumeHandler;
	} 
	
	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	} 

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public ConsumeGroup getConsumeGroup() {
		return consumeGroup;
	}

	public void setConsumeGroup(ConsumeGroup consumeGroup) {
		this.consumeGroup = consumeGroup;
	}

	public int getConsumeTimeout() {
		return consumeTimeout;
	}

	public MessageHandler getConsumeHandler() {
		return consumeHandler;
	}

	public Integer getConsumeWindow() {
		return consumeWindow;
	}

	public void setConsumeWindow(Integer consumeWindow) {
		this.consumeWindow = consumeWindow;
	}

	public MqClient getClient() {
		return client;
	} 
	
	
}