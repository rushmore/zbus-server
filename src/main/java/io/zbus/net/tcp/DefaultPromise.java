package io.zbus.net.tcp;
 
import java.util.UUID;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;

public class DefaultPromise<V> extends DefaultFuture<V> {
	private Promise<V> promise;
	private final String id;
	
	public DefaultPromise(EventExecutor executor) {
		super(new io.netty.util.concurrent.DefaultPromise<V>(executor));  
		this.promise = (io.netty.util.concurrent.DefaultPromise<V>) this.support;
		this.id = UUID.randomUUID().toString();
	} 
	 
	public String id() {
		return id;
	}
	
	public DefaultPromise<V> setSuccess(V result){
		promise.setSuccess(result);
    	return this;
    } 
	
    public boolean trySuccess(V result){
    	return promise.trySuccess(result);
    }
 
    public DefaultPromise<V> setFailure(Throwable cause){
    	promise.setFailure(cause);
    	return this;
    } 
    
    public boolean tryFailure(Throwable cause){
    	return promise.tryFailure(cause);
    }
 
    boolean setUncancellable(){
    	return promise.setUncancellable();
    }
}
