package io.zbus.mq.server;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.fastjson.JSON;

import io.zbus.mq.api.Message;
import io.zbus.mq.api.MqAdmin.Channel;
import io.zbus.mq.api.MqAdmin.Topic;
import io.zbus.mq.api.Protocol;
import io.zbus.mq.net.MessageAdaptor;
import io.zbus.net.Session;

public class MqAdaptor extends MessageAdaptor {
	AtomicInteger i = new AtomicInteger(0);
	public MqAdaptor() { 
		
		registerFilterHandler(new MessageHandler() { 
			@Override
			public void handle(Message msg, Session session) throws IOException {
				//System.out.println(msg);
			}
		});
		
		cmd(Protocol.DECLARE_TOPIC, new MessageHandler() { 
			@Override
			public void handle(Message msg, Session session) throws IOException {
				Message res = new Message();
				res.setCmd(Protocol.RESPONSE);
				res.setStatus(200);
				res.setId(msg.getId());
				
				Topic topic = new Topic();
				topic.setName("MyTopic" + System.currentTimeMillis());
				res.setJsonBody(JSON.toJSONBytes(topic));
				session.write(res);
			}
		}); 
		
		cmd(Protocol.DECLARE_CHANNEL, new MessageHandler() { 
			@Override
			public void handle(Message msg, Session session) throws IOException {
				Message res = new Message();
				res.setCmd(Protocol.RESPONSE);
				res.setStatus(200);
				res.setId(msg.getId());
				
				Channel channel = new Channel();
				channel.setTopic("MyTopic");
				channel.setChannel("default");
				res.setJsonBody(JSON.toJSONBytes(channel));
				session.write(res);
				if(i.incrementAndGet() % 10000 == 0){
					System.out.println(i);
				}
			}
		}); 
		
		cmd(Protocol.PRODUCE, new MessageHandler() { 
			@Override
			public void handle(Message msg, Session session) throws IOException {
				Message res = new Message();
				res.setStatus(200);
				res.setBody(String.format("OK %d", System.currentTimeMillis()));
				session.write(res);
			}
		}); 
	} 
	
	
	public static void main(String[] args) throws Exception {    
		MqServer server = new MqServer();
		
		try {
			server.start(15555); 
			server.join();
		} finally { 
			server.close();
		} 
	} 
}
