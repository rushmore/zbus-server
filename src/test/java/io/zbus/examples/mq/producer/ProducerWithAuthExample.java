package io.zbus.examples.mq.producer;

import io.zbus.mq.Broker;
import io.zbus.mq.Message;
import io.zbus.mq.Producer;
import io.zbus.mq.ProducerConfig; 

public class ProducerWithAuthExample { 
	public static void main(String[] args) throws Exception { 
		String token = "MyTopic_token";
		Broker broker = new Broker("localhost:15555", token);  //!!!Share it in Application!!!
		  
		ProducerConfig config = new ProducerConfig();
		config.setBroker(broker);
		config.setToken(token);
		
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
