package io.zbus.mq.api;

public interface Consumer extends MqAdmin {
	MqFuture<Void> consume(ChannelCtrl ctrl);   
	Message take(int timeout);  
	void start(); 
}
