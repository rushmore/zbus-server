package io.zbus.mq.api;

public interface CtrlMessageHandler {
	void onCtrl(String cmd, Message message);
}
