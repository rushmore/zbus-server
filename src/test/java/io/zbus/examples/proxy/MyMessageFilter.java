package io.zbus.examples.proxy;

import io.zbus.mq.Message;
import io.zbus.mq.MqClient;
import io.zbus.proxy.http.MessageFilter;

public class MyMessageFilter implements MessageFilter {

	@Override
	public boolean filter(Message msg, MqClient client) {
		System.out.println(msg); //do some more interesting stuff!
		
		return true; //true to continue handling
	}

}
