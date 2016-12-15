package io.zbus.mq.api;

public interface Broker extends MqInvoker{  
	MqInvoker createInvoker();
	void onServerJoin();
	void onServerLeave();
	void workingServers();
	void selectServer(); 
}
