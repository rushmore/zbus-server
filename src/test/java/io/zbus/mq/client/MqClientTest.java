package io.zbus.mq.client;
 
import io.zbus.mq.api.MqAdmin.Auth;
import io.zbus.mq.api.MqAdmin.Topic;
import io.zbus.mq.api.MqAdmin.TopicCtrl;
import io.zbus.mq.api.MqClient;
import io.zbus.mq.api.MqClient.MqFuture;
import io.zbus.net.IoDriver;
 
public class MqClientTest {
 
	public static void main(String[] args) throws Exception {
		IoDriver ioDriver = new IoDriver();
		
		MqClient client = new MqTcpClient("localhost:8080", ioDriver);  
		client.configAuth(new Auth(null, "xxxyyy"));
		
		
		TopicCtrl ctrl = new TopicCtrl();
		ctrl.setTopic("MyTopic");   
		
		MqFuture<Topic> res = client.declareTopic(ctrl);
		System.out.println(res.get());
		
		
		System.out.println("==done==");
		
		client.close();
		ioDriver.close();
	} 
}
