package io.zbus.examples.mq.producer;

import io.zbus.mq.Broker;
import io.zbus.mq.Message;
import io.zbus.mq.Producer;
import io.zbus.mq.ProducerConfig;
import io.zbus.transport.ServerAddress; 

public class ProducerWithSslExample { 
	public static void main(String[] args) throws Exception {  
		ServerAddress serverAddress = new ServerAddress("localhost:15555", true);
		serverAddress.setCertFile("ssl/zbus.crt"); 
		
		Broker broker = new Broker(); //!!!Share it in Application!!!
		broker.addTracker(serverAddress);
		  
		ProducerConfig config = new ProducerConfig();
		config.setBroker(broker);  
		
		Producer p = new Producer(config);
		p.declareTopic("MyTopic"); 
		 
		Message msg = new Message(); 
		msg.setTopic("MyTopic");
		//msg.setTag("oo.account.pp");
		msg.setBody("hello " + System.currentTimeMillis()); 
		
		Message res = p.publish(msg);
		System.out.println(res);   
		 
		broker.close();
	} 
}
