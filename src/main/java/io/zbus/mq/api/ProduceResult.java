package io.zbus.mq.api;

public class ProduceResult {
	public boolean sendOk;
	public boolean ackEnabled;
	public Long serverAckTime;
	public Throwable error;
}
