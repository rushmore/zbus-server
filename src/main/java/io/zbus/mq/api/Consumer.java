package io.zbus.mq.api;

public interface Consumer extends MqAdmin{
	Message take(int timeout);
	void onMessage();
	void start();
}
