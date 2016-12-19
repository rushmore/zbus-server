package io.zbus.net.tcp;
 
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GenericFutureListener;
import io.zbus.net.Future;
import io.zbus.net.FutureListener;
 

public class DefaultFuture<V> implements Future<V> {  
	private Map<Object, Object> listenerMap = new ConcurrentHashMap<Object, Object>();
	 
	protected final io.netty.util.concurrent.Future<V> support;
	
	public DefaultFuture(io.netty.util.concurrent.Future<V> support){
		this.support = support; 
	}
	  
	public DefaultFuture(EventExecutor executor) { 
		this(new DefaultPromise<V>(executor));    
	} 
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return support.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return support.isCancelled();
	}

	@Override
	public boolean isDone() {
		return support.isDone();
	}

	@Override
	public V get() throws InterruptedException, ExecutionException {
		return support.get();
	}

	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return support.get(timeout, unit);
	}

	@Override
	public boolean isSuccess() {
		return support.isSuccess();
	}

	@Override
	public boolean isCancellable() {
		return support.isCancellable();
	}

	@Override
	public Throwable cause() {
		return support.cause();
	}  
	
	@Override
	public DefaultFuture<V> addListener(final FutureListener<V> listener) {
		GenericFutureListener<io.netty.util.concurrent.Future<? super V>> supportListener = new GenericFutureListener<io.netty.util.concurrent.Future<? super V>>() {
			@Override
			public void operationComplete(io.netty.util.concurrent.Future<? super V> future) throws Exception {
				listener.operationComplete(DefaultFuture.this);
			}
		};
		
		listenerMap.put(listener, supportListener);
		support.addListener(supportListener);
		return this;
	}  
	
	@Override
	public DefaultFuture<V> removeListener(FutureListener<V> listener) {
		@SuppressWarnings("unchecked")
		GenericFutureListener<io.netty.util.concurrent.Future<? super V>> supportListener 
			= (GenericFutureListener<io.netty.util.concurrent.Future<? super V>>) listenerMap.get(listener);

		support.removeListener(supportListener);
		return this;
	}
	 

	@Override
	public DefaultFuture<V> sync() throws InterruptedException {
		support.sync();
		return this;
	}

	@Override
	public DefaultFuture<V> syncUninterruptibly() {
		support.syncUninterruptibly();
		return this;
	}

	@Override
	public DefaultFuture<V> await() throws InterruptedException {
		support.await();
		return this;
	}

	@Override
	public DefaultFuture<V> awaitUninterruptibly() {
		support.awaitUninterruptibly();
		return this;
	}

	@Override
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
		return support.await(timeout, unit); 
	}

	@Override
	public boolean await(long timeoutMillis) throws InterruptedException {
		return support.await(timeoutMillis); 
	}

	@Override
	public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
		return support.awaitUninterruptibly(timeout, unit); 
	}

	@Override
	public boolean awaitUninterruptibly(long timeoutMillis) {
		return support.awaitUninterruptibly(timeoutMillis); 
	}

	@Override
	public V getNow() {
		return support.getNow();
	}  
	
	
}
