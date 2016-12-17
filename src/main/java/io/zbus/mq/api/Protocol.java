package io.zbus.mq.api;

public interface Protocol {
	public static final String DATA = "data";
	public static final String ACK = "ack";
	public static final String CTRL = "ctrl";
	public static final String PRODUCE = "produce";
	public static final String CONSUME = "consume";
}
