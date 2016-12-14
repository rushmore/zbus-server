package io.zbus.net.tcp;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.GenericFutureListener;
import io.zbus.net.Future;
import io.zbus.net.FutureListener;
 

public class DefaultFuture<V> implements Future<V> { 
	private io.netty.util.concurrent.Future<V> support;
	
	public DefaultFuture(io.netty.util.concurrent.Future<V> support){
		this.support = support;
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
	 
	public Future<V> addListener2(final DefaultFutureListener<V> listener) {
		support.addListener(new GenericFutureListener<io.netty.util.concurrent.Future<? super V>>() {

			@Override
			public void operationComplete(io.netty.util.concurrent.Future<? super V> future) throws Exception {
				listener.operationComplete(DefaultFuture.this);
			}
		});
		return this;
	}
	
	@Override
	public DefaultFuture<V> addListener(final FutureListener<? extends Future<V>> listener) { 
		final DefaultFuture<V> thisFuture = this; 
		support.addListener(new GenericFutureListener<io.netty.util.concurrent.Future<? super V>>() {

			@Override
			public void operationComplete(io.netty.util.concurrent.Future<? super V> future) throws Exception {
				//listener.operationComplete(thisFuture);
			}
		});
		return this;
	}
	
	@Override
	public Future<V> addListeners(FutureListener<? extends Future<? super V>>... listeners) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<V> removeListener(FutureListener<? extends Future<? super V>> listener) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<V> removeListeners(FutureListener<? extends Future<? super V>>... listeners) {
		// TODO Auto-generated method stub
		return null;
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
		return support.getNow();
	} 
	 
	public static void main(String[] args) { 
		DefaultFuture<Long> future = new DefaultFuture<Long>(null);
		future.addListener(new DefaultFutureListener<Long>() { 
			@Override
			public void operationComplete(DefaultFuture<Long> future) throws Exception {
				// TODO Auto-generated method stub
				
			}
		}); 
	}
}
