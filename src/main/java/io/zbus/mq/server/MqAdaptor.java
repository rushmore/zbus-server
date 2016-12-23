package io.zbus.mq.server;

import java.io.IOException;

import com.alibaba.fastjson.JSON;

import io.zbus.mq.api.Message;
import io.zbus.mq.api.MqAdmin.ChannelDetails;
import io.zbus.mq.api.MqAdmin.Topic;
import io.zbus.mq.api.Protocol;
import io.zbus.mq.net.MessageAdaptor;
import io.zbus.net.Session;

public class MqAdaptor extends MessageAdaptor {
	
	public MqAdaptor() { 
		
		registerFilterHandler(new MessageHandler() { 
			@Override
			public void handle(Message msg, Session session) throws IOException {
				System.out.println(msg);
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
				topic.name = "MyTopic" + System.currentTimeMillis();
				res.setJsonBody(JSON.toJSONBytes(topic));
				session.write(res);
			}
		}); 
		
		cmd(Protocol.DECLARE_CONSUME_GROUP, new MessageHandler() { 
			@Override
			public void handle(Message msg, Session session) throws IOException {
				Message res = new Message();
				res.setCmd(Protocol.RESPONSE);
				res.setStatus(200);
				res.setId(msg.getId());
				
				ChannelDetails channel = new ChannelDetails();
				channel.topic = "MyTopic";
				channel.channel = "default";
				res.setJsonBody(JSON.toJSONBytes(channel));
				session.write(res);
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
}
