package io.zbus.mq.api;

public interface Consumer extends MqAdmin{
	void subscribe(String topic);
	Message take(int timeout);
	void onMessage();
	void start();
}
