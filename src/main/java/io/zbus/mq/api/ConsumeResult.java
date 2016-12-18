package io.zbus.mq.api;

public class ConsumeResult {
	public boolean sendOk;
	public boolean ackEnabled;
	public Long serverAckTime;
	public Throwable error;
}
