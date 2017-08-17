package io.zbus.examples.transport.inproc;

import io.zbus.mq.Message;
import io.zbus.mq.MqClient;
import io.zbus.mq.server.MqServer;

public class ConsumerExample {   
	 
	public static void main(String[] args) throws Exception {     
		MqServer mqServer = new MqServer("conf/zbus1.xml");  
		mqServer.start();
		  
		
		MqClient client = new MqClient(mqServer); 
		Message msg = new Message();
		msg.setTopic("MyTopic");
		msg.setBody("hello from inproc");
		client.produce(msg);
		
		Message res = client.consume("MyTopic");
		System.out.println(res);
		
		client.close();
		mqServer.close();
	}  
}
