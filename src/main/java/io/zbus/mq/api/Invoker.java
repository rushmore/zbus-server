package io.zbus.mq.api;

import java.io.Closeable;

public interface Invoker extends Closeable{ 
	Message invoke(Message message);  
}
