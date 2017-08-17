package io.zbus.examples.transport.inproc;

import java.io.IOException;

import io.zbus.transport.MessageHandler;
import io.zbus.transport.Session;
import io.zbus.transport.http.Message;
import io.zbus.transport.http.MessageAdaptor;
import io.zbus.transport.http.MessageClient;
import io.zbus.transport.http.MessageServer;

public class MessageClientExample {
 
	public static void main(String[] args) throws Exception {   
		MessageAdaptor adaptor = new MessageAdaptor();
		
		adaptor.cmd("", new MessageHandler<Message>() { 
			@Override
			public void handle(Message msg, Session session) throws IOException {  
				Message res = new Message();
				res.setId(msg.getId()); //match the ID for response
				res.setStatus(200);
				res.setBody("Hello world");
				session.write(res);
			}
		});
		 
		MessageServer server = new MessageServer();   
		server.start(80, adaptor);  
		 
		MessageClient client = new MessageClient(adaptor); 
		Message req = new Message(); 
		Message res = client.invokeSync(req);   
		System.out.println(res);
		
		client.close();
		server.close();
	} 
}
