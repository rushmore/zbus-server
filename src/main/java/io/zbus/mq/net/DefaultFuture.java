package io.zbus.mq.net;
 
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.zbus.mq.Future;
import io.zbus.mq.FutureListener; 
 

class DefaultFuture<V, W> implements Future<V> {  
	protected Map<Object, Object> listenerMap = new ConcurrentHashMap<Object, Object>();
	protected final io.zbus.net.Future<W> support;
	
	public DefaultFuture(io.zbus.net.Future<W> support){
		this.support = support; 
	} 
	
	@SuppressWarnings("unchecked")
	public V convert(W result){ 
		return (V)result;
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
		return convert(support.get());
	}

	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException { 
		return convert(support.get(timeout, unit));
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
	public Future<V> addListener(final FutureListener<V> listener) { 
		io.zbus.net.FutureListener<W> supportListener = new io.zbus.net.FutureListener<W>() {   
			@Override
			public void operationComplete(io.zbus.net.Future<W> future) throws Exception {
				listener.operationComplete(DefaultFuture.this);
			}
		};
		listenerMap.put(listener, supportListener);
		support.addListener(supportListener);
		return this;
	}

	@Override
	public Future<V> removeListener(FutureListener<V> listener) {
		@SuppressWarnings("unchecked")
		io.zbus.net.FutureListener<W> supportListener = (io.zbus.net.FutureListener<W>) listenerMap.get(listener); 
		if(supportListener == null){
			throw new IllegalStateException("listener not registered");
		}
		support.removeListener(supportListener);
		return this;
	}

	@Override
	public Future<V> sync() throws InterruptedException {
		support.sync();
		return this;
	}

	@Override
	public Future<V> syncUninterruptibly() {
		support.syncUninterruptibly();
		return this;
	}

	@Override
	public Future<V> await() throws InterruptedException {
		support.await();
		return this;
	}

	@Override
	public Future<V> awaitUninterruptibly() {
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
		return convert(support.getNow());
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) { 
		return support.cancel(mayInterruptIfRunning);
	}
	 
	
}
