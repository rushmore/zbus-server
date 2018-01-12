package io.zbus.rpc;


public interface RpcCallback<T> { 
	void onSuccess(T result);  
	void onError(Exception error);  
}