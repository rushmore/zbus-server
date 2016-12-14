package io.zbus.mq.api;

import java.io.Closeable;

public interface Invoker extends Closeable{ 
	Future<Message> invoke(Message message);  
}
