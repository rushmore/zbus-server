package io.zbus.mq.server;

import java.io.IOException;

import io.zbus.mq.api.Message;
import io.zbus.mq.net.MessageAdaptor;
import io.zbus.net.Session;

public class MqAdaptor extends MessageAdaptor {
	
	public MqAdaptor() { 
		cmd("produce", new MessageHandler() { 
			@Override
			public void handle(Message msg, Session session) throws IOException {
				System.out.println(msg);
				Message res = new Message();
				res.setStatus(200);
				msg.setBody(String.format("OK %d", System.currentTimeMillis()));
				session.write(res);
			}
		}); 
	} 
}
