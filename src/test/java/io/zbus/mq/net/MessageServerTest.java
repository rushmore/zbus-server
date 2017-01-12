package io.zbus.mq.net;

import java.io.IOException;

import io.zbus.mq.Message;
import io.zbus.net.Server;
import io.zbus.net.Session;

public class MessageServerTest {
	
	public static void main(String[] args) throws Exception {    
		Server server = new MessageServer();
		
		try {
			MessageAdaptor ioAdaptor = new MessageAdaptor(); 
			ioAdaptor.cmd("", new MessageHandler() { 
				@Override
				public void handle(Message msg, Session session) throws IOException {
					msg.setStatus(200);
					msg.setBody(""+System.currentTimeMillis());
					session.write(msg);
				}
			});
			
			server.start(15555, ioAdaptor);  
			server.join();
		} finally { 
			server.close();
		} 
	}

}
