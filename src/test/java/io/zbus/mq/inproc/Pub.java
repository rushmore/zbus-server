package io.zbus.mq.inproc;

import java.util.concurrent.atomic.AtomicInteger;

import io.zbus.mq.MqClient;
import io.zbus.mq.MqServer;
import io.zbus.mq.MqServerConfig;
import io.zbus.transport.Message;

public class Pub {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		MqServer server = new MqServer(new MqServerConfig());
		
		MqClient client = new MqClient(server); 
		
		String mq = "MyMQ";
		
		Message create = new Message();
		create.setHeader("cmd", "create");
		create.setHeader("mq", mq); 
		create.setHeader("mqType", "disk");
		client.invoke(create, res->{
			System.out.println(res);
		});
		Thread.sleep(100);
		
		AtomicInteger count = new AtomicInteger(0);  
		for (int i = 0; i < 200000; i++) {   
			Message msg = new Message();
			msg.setHeader("cmd", "pub"); //Publish
			msg.setHeader("mq", mq);
			msg.setBody(i);  
			
			client.invoke(msg, res->{
				if(count.getAndIncrement() % 10000 == 0) {
					System.out.println(res); 
				}
			});
		} 
		//ws.close(); 
	}
}
