package io.zbus.mq;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;

public class ConsumeThread implements Closeable{
	private static final Logger log = LoggerFactory.getLogger(ConsumeThread.class);  
	protected final MqClient client;
	
	protected String topic;  
	protected String token;
	protected ConsumeGroup consumeGroup;
	protected int consumeTimeout = 10000;
	protected Integer consumeWindow;
	
	protected ExecutorService consumeRunner;
	protected MessageHandler messageHandler;
	
	protected AtomicReference<CountDownLatch> pause = new AtomicReference<CountDownLatch>(new CountDownLatch(0));
	
	protected Thread consumeThread;
	 
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
			pause.set(new CountDownLatch(1));
		}
		
		if(this.consumeGroup == null){
			this.consumeGroup = new ConsumeGroup();
			consumeGroup.setGroupName(this.topic);
		}  
		this.client.setToken(token);
		this.client.setInvokeTimeout(consumeTimeout);
		try {
			this.client.declareGroup(topic, consumeGroup);
		} catch (IOException e) { 
			log.error(e.getMessage(), e);
		} catch (InterruptedException e) { 
			log.error(e.getMessage(), e);
		}
		
		
		consumeThread = new Thread(new Runnable() { 
			@Override
			public void run() { 
				ConsumeThread.this.run();
			}
		});
		consumeThread.start();
	}
	
	public void pause(){
		try {
			client.unconsume(topic, this.consumeGroup.getGroupName()); //stop consuming in serverside
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}  
		
		pause.set(new CountDownLatch(1));
	}
	
	public void resume(){
		pause.get().countDown();
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
	 
	protected void run() {   
		while (true) {
			try { 
				final Message msg;
				try {
					pause.get().await(); //check is paused
					msg = take();
					if(msg == null) continue;
					
					if(messageHandler == null){
						throw new IllegalStateException("Missing ConsumeHandler");
					}
					
					if(consumeRunner == null){
						try{
							messageHandler.handle(msg, client);
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
					} else {
						consumeRunner.submit(new Runnable() { 
							@Override
							public void run() {
								try{
									messageHandler.handle(msg, client);
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
		consumeThread.interrupt();  
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

	public void setMessageHandler(MessageHandler messageHandler) {
		this.messageHandler = messageHandler;
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

	public MessageHandler getMessageHandler() {
		return messageHandler;
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