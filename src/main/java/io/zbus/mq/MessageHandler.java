package io.zbus.mq;

import java.io.IOException;

public interface MessageHandler{
	void handle(Message msg, MqClient client) throws IOException;
}