package io.zbus.mq.api;

import java.io.Closeable;

public interface Invoker extends Closeable{ 
	MqFuture<Message> invoke(Message message);  
}
