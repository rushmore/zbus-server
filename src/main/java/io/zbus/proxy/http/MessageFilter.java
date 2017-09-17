package io.zbus.proxy.http;

import io.zbus.mq.Message;
import io.zbus.mq.MqClient;

public interface MessageFilter {
	/**
	 * Handle message before sending to target
	 * 
	 * @param msg message to filter
	 * @param client client connection to zbus
	 * 
	 * @return true to continue sending, false to discard
	 */
	boolean filter(Message msg, MqClient client);
}
