package io.zbus.examples.proxy;

import java.io.IOException;

import io.zbus.mq.Message;
import io.zbus.mq.MqClient;
import io.zbus.proxy.http.MessageFilter;

public class ForbbidenMessageFilter implements MessageFilter {

	@Override
	public boolean filter(Message msg, MqClient client) {
		Message res = new Message();
		res.setStatus(403);
		res.setId(msg.getId());  //MsgID match
		res.setReceiver(msg.getSender()); //Route back to sender
		res.setBody("Forbbiden");
		
		try {
			client.route(res);
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return false;
	}

}
