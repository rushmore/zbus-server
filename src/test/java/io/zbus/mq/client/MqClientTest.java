package io.zbus.mq.client;
 
import io.zbus.mq.api.MqAdmin.Auth;
import io.zbus.mq.api.MqAdmin.ConsumeGroupDetails;
import io.zbus.mq.api.MqAdmin.ConsumeGroupDeclare;
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
		
		
		ConsumeGroupDeclare channelDeclare = new ConsumeGroupDeclare();
		channelDeclare.topic = "MyTopic";
		channelDeclare.consumeGroup = "default";
		channelDeclare.exclusive = true;
		channelDeclare.deleteOnExit = true; 
		MqFuture<ConsumeGroupDetails> mf = client.declareConsumeGroup(channelDeclare);
		System.out.println(mf.get()); 
		 
		
		System.out.println("==done==");
		
		client.close();
		ioDriver.close();
	} 
}
