package io.zbus.net;

import java.util.EventListener;
 

public interface FutureListener<V> extends EventListener { 
	void operationComplete(Future<V> future) throws Exception;
}
