package io.zbus.mq;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.zbus.transport.Message;

public class Pub { 
	
	public static MqClient buildInproClient() {
		MqServer server = new MqServer(new MqServerConfig());
		return new MqClient(server);
	}
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		MqClient client = new MqClient("wss://111.230.136.74"); 
		
		//MqClient client = buildInproClient();
		
		client.heartbeat(30, TimeUnit.SECONDS); 
		final String mq = "MyMQ", mqType = Protocol.MEMORY;
		
		//1) Create MQ if necessary
		Message req = new Message();
		req.setHeader("cmd", "create");  //Create
		req.setHeader("mq", mq); 
		req.setHeader("mqType", mqType); //disk|memory|db
		
		client.invoke(req);
		
		AtomicInteger count = new AtomicInteger(0);  
		for (int i = 0; i < 100; i++) {   
			//2) Publish Message
			Message msg = new Message();
			msg.setHeader("cmd", "pub");  //Publish
			msg.setHeader("mq", mq);
			msg.setBody(i);    //set business data in body
			
			client.invoke(msg, res->{ //async call
				if(count.getAndIncrement() % 10000 == 0) {
					System.out.println(res); 
				}
			});
		}  
	}
}
