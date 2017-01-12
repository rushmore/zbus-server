package io.zbus.mq;

import java.util.EventListener;

public interface FutureListener<V> extends EventListener { 
	void operationComplete(Future<V> future) throws Exception;
}