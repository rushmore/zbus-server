package io.zbus.net;

import java.util.EventListener;
 

public interface FutureListener<F extends Future<?>> extends EventListener { 
	void operationComplete(F future) throws Exception;
}
