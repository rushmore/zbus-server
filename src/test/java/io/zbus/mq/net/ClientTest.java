package io.zbus.mq.net;
 
import java.io.IOException;

import io.zbus.mq.api.Message;
import io.zbus.net.Client.DataHandler;
import io.zbus.net.IoDriver;
import io.zbus.net.Session;
 
public class ClientTest {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		IoDriver ioDriver = new IoDriver();
		
		MessageClient client = new MessageClient("localhost:15555", ioDriver); 
		
		Message message = new Message();
		message.setCmd("produce");
		message.setTopic("Hong");
		message.setHeader("mq", "hong");
		
		
		client.onData(new DataHandler<Message>() { 
			int i=0;
			@Override
			public void onData(Message data, Session session) throws IOException {
				System.out.println(++i + " " + data);
			}
		});  
		 
		client.connect();
		for(int i=0;i<100;i++){
			System.out.println("main"); 
			client.send(message);
		} 
		//client.close();
		//ioDriver.close();
	} 
}
