package io.zbus.mq.api;

import java.io.Closeable;

public interface MqInvoker extends Closeable{ 
	MqFuture<Message> invoke(Message message);  
}
