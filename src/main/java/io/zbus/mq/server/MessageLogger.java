package io.zbus.mq.server;

import io.zbus.mq.Message;
import io.zbus.transport.Session;


public interface MessageLogger { 
	void log(Message message, Session session);
}

