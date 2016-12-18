package io.zbus.mq.client;
 
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.zbus.mq.api.MqFuture;
import io.zbus.mq.api.MqFutureListener;
import io.zbus.net.Future;
 

public class DefaultMqFuture<V> implements MqFuture<V> {   
	protected final Future<V> support;
	
	public DefaultMqFuture(Future<V> support){
		this.support = support; 
	}

	@Override
	public boolean isCancelled() { 
		return false;
	}

	@Override
	public boolean isDone() { 
		return false;
	}

	@Override
	public V get() throws InterruptedException, ExecutionException { 
		return null;
	}

	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException { 
		return null;
	}

	@Override
	public boolean isSuccess() { 
		return false;
	}

	@Override
	public boolean isCancellable() { 
		return false;
	}

	@Override
	public Throwable cause() { 
		return null;
	}

	@Override
	public MqFuture<V> addListener(MqFutureListener<V> listener) { 
		return null;
	}

	@Override
	public MqFuture<V> removeListener(MqFutureListener<V> listener) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MqFuture<V> sync() throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MqFuture<V> syncUninterruptibly() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MqFuture<V> await() throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MqFuture<V> awaitUninterruptibly() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean await(long timeoutMillis) throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean awaitUninterruptibly(long timeoutMillis) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public V getNow() { 
		return null;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) { 
		return false;
	}
	 
	
}
