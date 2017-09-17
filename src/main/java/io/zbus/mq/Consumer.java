package io.zbus.mq;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Broker.ServerNotifyListener;
import io.zbus.mq.Broker.ServerSelector;
import io.zbus.transport.ServerAddress; 

public class Consumer extends MqAdmin implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(Consumer.class);  
	private ServerSelector consumeServerSelector; 
	
	protected String topic; 
	protected ConsumeGroup consumeGroup; 
	protected Integer consumeWindow; 
	protected int consumeTimeout; 
	
	private ExecutorService consumeRunner;  
	private MessageHandler messageHandler;
	private int connectionCount;
	private int consumeRunnerPoolSize; 
	private int maxInFlightMessage;   
	
	private boolean started;
	
	private Map<ServerAddress, ConsumeThreadGroup> consumeThreadGroupMap = new ConcurrentHashMap<ServerAddress, ConsumeThreadGroup>(); 

	public Consumer(ConsumerConfig config) {
		super(config); 
		
		this.topic = config.getTopic(); 
		this.consumeGroup = config.getConsumeGroup();
		if(this.consumeGroup == null){
			this.consumeGroup = new ConsumeGroup();
			this.consumeGroup.setGroupName(this.topic);
		}
		
		if(this.consumeGroup.getMask() == null){ //TODO
			this.consumeGroup.setMask(config.getTopicMask());
		}
		this.consumeWindow = config.getConsumeWindow();
		this.consumeTimeout = config.getConsumeTimeout();
		
		this.messageHandler = config.getMessageHandler();
		this.consumeRunnerPoolSize = config.getConsumeRunnerPoolSize();
		this.connectionCount = config.getConnectionCount();
		this.maxInFlightMessage = config.getMaxInFlightMessage();
		
		this.consumeServerSelector = config.getConsumeServerSelector();
		if(this.consumeServerSelector == null){
			this.consumeServerSelector = new DefaultConsumeServerSelector();
		}
	} 
	
	public synchronized void start() throws IOException{
		start(false);
	}

	public synchronized void start(final boolean pauseOnStart) throws IOException{  
		if(started) return;
		
		if(this.messageHandler == null){
			throw new IllegalArgumentException("ConsumeHandler and MessageProcessor are both null");
		} 
		
		int n = consumeRunnerPoolSize;
		consumeRunner = new ThreadPoolExecutor(n, n, 120, TimeUnit.SECONDS, 
				new LinkedBlockingQueue<Runnable>(maxInFlightMessage),
				new ThreadPoolExecutor.CallerRunsPolicy());  
		
		Message msg = new Message();
		msg.setTopic(topic);
		MqClientPool[] pools = broker.selectClient(consumeServerSelector, msg); 
		
		for(MqClientPool pool : pools){
			startConsumeThreadGroup(pool, pauseOnStart);
		}
		
		broker.addServerNotifyListener(new ServerNotifyListener() { 
			@Override
			public void onServerLeave(ServerAddress serverAddress) { 
				ConsumeThreadGroup group = consumeThreadGroupMap.remove(serverAddress);
				if(group != null){
					try {
						log.info("Server(" + serverAddress + ") left, clear consumeThreads connecting to it");
						group.close();
					} catch (IOException e) {
						log.error(e.getMessage(), e);
					}
				}
			} 
			@Override
			public void onServerJoin(MqClientPool pool) { 
				startConsumeThreadGroup(pool, pauseOnStart);
			}
		});
		
		started = true;
	} 
	
	public void pause(){
		for(ConsumeThreadGroup consumerThreadGroup : consumeThreadGroupMap.values()){
			consumerThreadGroup.pause();
		}
	}
	
	public void resume(){
		for(ConsumeThreadGroup consumerThreadGroup : consumeThreadGroupMap.values()){
			consumerThreadGroup.resume();
		}
	}
	 
	private void startConsumeThreadGroup(MqClientPool pool, boolean pasuseOnStart){
		if(consumeThreadGroupMap.containsKey(pool.serverAddress())){
			return;
		}
		ConsumeThreadGroup group = new ConsumeThreadGroup(pool);
		consumeThreadGroupMap.put(pool.serverAddress(), group);
		group.start(pasuseOnStart); 
	}
	
	public void start(MessageHandler consumerHandler) throws IOException{
		setMessageHandler(consumerHandler);
		start();
	} 
	
	@Override
	public void close() throws IOException { 
		if(this.consumeThreadGroupMap != null){ 
			for(ConsumeThreadGroup consumerThreadGroup : consumeThreadGroupMap.values()){
				consumerThreadGroup.close();
			}
			this.consumeThreadGroupMap.clear();
			this.consumeThreadGroupMap = null;
		} 
		if(consumeRunner != null){
			consumeRunner.shutdown();
			consumeRunner = null;
		}
	}
	
	public void setMessageHandler(MessageHandler messageHandler){
		this.messageHandler = messageHandler;
	} 
	
	private class ConsumeThreadGroup implements Closeable{ 
		private ConsumeThread[] threads;  
		ConsumeThreadGroup(MqClientPool pool){ 
			threads = new ConsumeThread[connectionCount];
			for(int i=0;i<connectionCount;i++){
				MqClient clieint = pool.createClient();
				ConsumeThread thread = threads[i] = new ConsumeThread(clieint); 
				thread.setTopic(topic); 
				thread.setConsumeGroup(consumeGroup); 
				thread.setToken(token); 
				thread.setConsumeRunner(consumeRunner); 
				thread.setConsumeTimeout(consumeTimeout);
				
				thread.setMessageHandler(messageHandler); 
			}
		} 
		
		public void start(boolean pauseOnStart){
			for(ConsumeThread thread : threads){
				thread.start(pauseOnStart);
			}
		}
		
		public void pause(){
			for(ConsumeThread thread : threads){
				thread.pause();
			}
		}
		
		public void resume(){
			for(ConsumeThread thread : threads){
				thread.resume();
			}
		}
		
		public void close() throws IOException{
			for(ConsumeThread thread : threads){
				thread.close();
				thread.getClient().close();
			}
		} 
	} 

	public ServerSelector getConsumeServerSelector() {
		return consumeServerSelector;
	} 

	public void setConsumeServerSelector(ServerSelector consumeServerSelector) {
		this.consumeServerSelector = consumeServerSelector;
	} 
	
	public static class DefaultConsumeServerSelector implements ServerSelector{ 
		@Override
		public ServerAddress[] select(BrokerRouteTable table, Message msg) { 
			return table.serverTable().keySet().toArray(new ServerAddress[0]); 
		} 
	}  
}
