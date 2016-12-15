package io.zbus.mq.api;

import java.util.concurrent.TimeUnit;

public interface MqFuture<V> extends java.util.concurrent.Future<V> {
	 
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
