package io.zbus.mq.api;

import java.util.EventListener;
 

public interface MqFutureListener<V> extends EventListener { 
	void operationComplete(MqFuture<V> future) throws Exception;
}
