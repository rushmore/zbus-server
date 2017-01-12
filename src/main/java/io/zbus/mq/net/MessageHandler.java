package io.zbus.mq.net;

import java.io.IOException;

import io.zbus.mq.Message;
import io.zbus.net.Session;

public interface MessageHandler {
	void handle(Message msg, Session session) throws IOException;   
}