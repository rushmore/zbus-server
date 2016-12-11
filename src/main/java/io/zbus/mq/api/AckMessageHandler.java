package io.zbus.mq.api;

public interface AckMessageHandler {
	void onAck(String cmd, Message message);
}
