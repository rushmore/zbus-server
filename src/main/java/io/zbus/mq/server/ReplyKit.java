package io.zbus.mq.server;

import java.io.IOException;

import io.zbus.kit.JsonKit;
import io.zbus.mq.Message;
import io.zbus.transport.Session;

public class ReplyKit { 
	
	public static void reply(Message req, Message res, Session session) throws IOException {
		res.setId(req.getId());
		res.setTopic(req.getTopic());  
		if(res.getStatus() == null){
			res.setStatus(200);
		}
		session.write(res);
	}
	
	public static void reply200(Message message, Session session) throws IOException {
		Message res = new Message();
		res.setId(message.getId());
		res.setTopic(message.getTopic());
		res.setStatus(200);
		res.setBody("" + System.currentTimeMillis());

		session.write(res);
	}
	
	public static void replyJson(Message msg, Session session, Object object) throws IOException {
		Message res = new Message();
		res.setId(msg.getId());
		res.setTopic(msg.getTopic());
		res.setStatus(200);
		res.setJsonBody(JsonKit.toJSONString(object)); 

		session.write(res);
	}

	public static void reply404(Message msg, Session session) throws IOException {
		Message res = new Message();
		String topic = msg.getTopic();
		res.setId(msg.getId());
		res.setStatus(404);
		res.setTopic(topic);
		res.setBody(String.format("404: Topic(%s) Not Found", topic));

		session.write(res);
	}
	
	public static void reply404(Message msg, Session session, String hint) throws IOException {
		Message res = new Message();
		String topic = msg.getTopic();
		res.setId(msg.getId());
		res.setStatus(404);
		res.setTopic(topic);
		res.setBody(String.format("404: %s", hint));

		session.write(res);
	}
	
	public static void reply502(Message msg, Session session) throws IOException {
		Message res = new Message();
		String mqName = msg.getTopic();
		res.setId(msg.getId());
		res.setStatus(502);
		res.setTopic(mqName);
		res.setBody(String.format("502: Service(%s) Down", mqName));

		session.write(res);
	}


	public static void reply403(Message msg, Session session) throws IOException {
		Message res = new Message();
		String mqName = msg.getTopic();
		res.setId(msg.getId());
		res.setStatus(403);
		res.setTopic(mqName);
		res.setBody(String.format("403: Topic(%s) Forbbiden", mqName));

		session.write(res);
	} 
	
	public static void reply400(Message msg, Session session, String hint) throws IOException {
		Message res = new Message();
		res.setId(msg.getId());
		res.setStatus(400);
		res.setTopic(msg.getTopic());
		res.setBody(String.format("400: Bad Format, %s", hint));
		session.write(res);
	}
	
	public static void reply500(Message msg, Session session, Exception ex) throws IOException {
		Message res = new Message();
		res.setId(msg.getId());
		res.setStatus(500);
		res.setTopic(msg.getTopic());
		res.setBody(String.format("500: Exception Caught, %s",ex));
		session.write(res);
	}  
}
