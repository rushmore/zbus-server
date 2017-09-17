package io.zbus.examples.transport.tcp;

import java.io.IOException;

import io.zbus.transport.MessageHandler;
import io.zbus.transport.Session;
import io.zbus.transport.http.Message;
import io.zbus.transport.http.MessageAdaptor;
import io.zbus.transport.http.MessageServer;

public class MessageServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) {   
		
		MessageAdaptor adaptor = new MessageAdaptor();
		
		adaptor.url("/", new MessageHandler<Message>() { 
			@Override
			public void handle(Message msg, Session session) throws IOException {  
				System.out.println(msg);
				Message res = new Message();
				res.setId(msg.getId()); //match the ID for response
				res.setStatus(200);
				res.setBody("Hello world");
				session.write(res);
			}
		});  
		
		MessageServer server = new MessageServer();   
		server.start(80, adaptor);  
	} 
}
