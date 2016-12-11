package io.zbus.mq.api;

public interface DataMessageHandler {
	void onData(Message message);
}
