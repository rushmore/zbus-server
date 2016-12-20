package io.zbus.mq.client;
 
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.zbus.mq.api.MqClient.MqFuture;
import io.zbus.mq.api.MqClient.MqFutureListener;
import io.zbus.net.Future;
import io.zbus.net.FutureListener;
 

public class DefaultMqFuture<V> implements MqFuture<V> {  
	private Map<Object, Object> listenerMap = new ConcurrentHashMap<Object, Object>();
	protected final Future<V> support;
	
	public DefaultMqFuture(Future<V> support){
		this.support = support; 
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
	public MqFuture<V> addListener(final MqFutureListener<V> listener) { 
		FutureListener<V> supportListener = new FutureListener<V>() { 
			@Override
			public void operationComplete(Future<V> future) throws Exception {
				listener.operationComplete(DefaultMqFuture.this);
			}
		};
		listenerMap.put(listener, supportListener);
		support.addListener(supportListener);
		return this;
	}

	@Override
	public MqFuture<V> removeListener(MqFutureListener<V> listener) {
		@SuppressWarnings("unchecked")
		FutureListener<V> supportListener = (FutureListener<V>) listenerMap.get(listener); 
		if(supportListener == null){
			throw new IllegalStateException("listener not registered");
		}
		support.removeListener(supportListener);
		return this;
	}

	@Override
	public MqFuture<V> sync() throws InterruptedException {
		support.sync();
		return this;
	}

	@Override
	public MqFuture<V> syncUninterruptibly() {
		support.syncUninterruptibly();
		return this;
	}

	@Override
	public MqFuture<V> await() throws InterruptedException {
		support.await();
		return this;
	}

	@Override
	public MqFuture<V> awaitUninterruptibly() {
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

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) { 
		return support.cancel(mayInterruptIfRunning);
	}
	 
	
}
