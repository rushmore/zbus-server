package io.zbus.mq.client;
 
import io.zbus.mq.api.MqAdmin.Auth;
import io.zbus.mq.api.MqAdmin.ChannelDetails;
import io.zbus.mq.api.MqAdmin.ChannelDeclare;
import io.zbus.mq.api.MqAdmin.Topic;
import io.zbus.mq.api.MqAdmin.TopicDeclare;
import io.zbus.mq.api.MqClient;
import io.zbus.mq.api.MqFuture;
import io.zbus.net.IoDriver;
 
public class MqClientTest {
 
	public static void main(String[] args) throws Exception {
		IoDriver ioDriver = new IoDriver();
		
		final MqClient client = new TcpMqClient("localhost:8080", ioDriver);  
		client.configAuth(new Auth());
		
		TopicDeclare topicDeclare = new TopicDeclare();
		topicDeclare.topic = "MyTopic";
		topicDeclare.rpcFlag = true; 
		MqFuture<Topic> res = client.declareTopic(topicDeclare);
		System.out.println(res.get()); 
		
		
		ChannelDeclare channelDeclare = new ChannelDeclare();
		channelDeclare.topic = "MyTopic";
		channelDeclare.channel = "default";
		channelDeclare.exclusive = true;
		channelDeclare.deleteOnExit = true; 
		MqFuture<ChannelDetails> mf = client.declareChannel(channelDeclare);
		System.out.println(mf.get()); 
		 
		
		System.out.println("==done==");
		
		client.close();
		ioDriver.close();
	} 
}
