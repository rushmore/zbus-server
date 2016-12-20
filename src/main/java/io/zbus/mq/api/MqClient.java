package io.zbus.mq.api;

import java.io.Closeable;
import java.util.EventListener;
import java.util.concurrent.TimeUnit;

public interface MqClient extends MqAdmin, Closeable{    
     
    void onProduceAck(AckHandler handler); 
    void onConsumeAck(AckHandler handler);  
	void onStream(StreamHandler handler);  
	void onQuit(QuitHandler handler); 
	
	MqFuture<ProduceResult> produce(Message message); 
	MqFuture<ConsumeResult> consume(ConsumeCtrl ctrl);   
	Message take(int timeout);   
	
	
	
	public static interface AckHandler {
		void onAck(Message message);
	} 

	public static interface StreamHandler {
		void onStream(Message message);
	} 
	
	public static interface QuitHandler {
		void onQuit(Message message);
	}
	
	public static interface MqFuture<V> extends java.util.concurrent.Future<V> {
		 
	    boolean isSuccess(); 
	    boolean isCancellable(); 
	    Throwable cause();
	 
	    MqFuture<V> addListener(MqFutureListener<V> listener);   
	    MqFuture<V> removeListener(MqFutureListener<V> listener);  

	    MqFuture<V> sync() throws InterruptedException; 
	    MqFuture<V> syncUninterruptibly(); 
	    
	    MqFuture<V> await() throws InterruptedException; 
	    MqFuture<V> awaitUninterruptibly(); 
	    boolean await(long timeout, TimeUnit unit) throws InterruptedException; 
	    boolean await(long timeoutMillis) throws InterruptedException; 
	    boolean awaitUninterruptibly(long timeout, TimeUnit unit); 
	    boolean awaitUninterruptibly(long timeoutMillis); 
	    
	    V getNow(); 
	    
	    @Override
	    boolean cancel(boolean mayInterruptIfRunning);
	}
	
	public static interface MqFutureListener<V> extends EventListener { 
		void operationComplete(MqFuture<V> future) throws Exception;
	}

	public static class ProduceResult {
		public boolean sendOk;
		public boolean ackEnabled;
		public Long serverAckTime;
		public Throwable error;
	}
	
	public static class ConsumeCtrl {
		public String topic;
		public String channel; 
		
		public String messageFilter;
		public Long consumeStartOffset; //valid only for single MqClient
		public Long consumeStartTime;
		public Boolean consumeStartDefault;    
	}
	
	public static class ConsumeResult {
		public boolean sendOk;
		public boolean ackEnabled;
		public Long serverAckTime;
		public Throwable error;
	}
}