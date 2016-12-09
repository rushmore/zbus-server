package io.zbus.mq.api;

public interface MessageHandler {
	void onMessage(Message message);
}
