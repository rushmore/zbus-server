package io.zbus.mq.net;

import io.zbus.mq.api.Message;
import io.zbus.mq.net.MessageAdaptor;
import io.zbus.mq.net.MessageServer;
import io.zbus.mq.net.MessageAdaptor.MessageProcessor;
import io.zbus.net.Server;

public class MessageServerExample {
	
	public static void main(String[] args) throws Exception {    
		Server server = new MessageServer();
		
		try {
			MessageAdaptor ioAdaptor = new MessageAdaptor(); 
			ioAdaptor.url("/", new MessageProcessor() { 
				@Override
				public Message process(Message request) {  
					Message res = new Message();
					res.setStatus(200);
					res.setBody("hello: " + System.currentTimeMillis());
					return res;
				}
			});   
			
			server.start(8080, ioAdaptor);  
			server.join();
		} finally { 
			server.close();
		} 
	}

}
