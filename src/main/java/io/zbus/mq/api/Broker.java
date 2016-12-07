package io.zbus.mq.api;

public interface Broker extends Invoker{  
	Invoker createInvoker();
	void onServerJoin();
	void onServerLeave();
	void workingServers();
	void selectServer(); 
}
