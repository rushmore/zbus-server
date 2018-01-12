package io.zbus.mq.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.zbus.kit.FileKit;
import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.mq.Message;
import io.zbus.transport.Session;

public class ReplyKit { 
	private static final Map<String, Object> EMPTY_MODEL = new HashMap<String, Object>(); 
	public static void replyTemplate(Message req, Session session, 
			String fileName, Map<String, Object> model) throws IOException {
		Message res = new Message();
		res.setStatus(200);
		res.setId(req.getId());
		res.setHeader("content-type", "text/html"); 
		String body = FileKit.loadFile(fileName, model);
		res.setBody(body);
		session.write(res);  
	}
	
	public static void replyTemplate(Message req, Session session, String fileName) throws IOException {
		replyTemplate(req, session, fileName, EMPTY_MODEL);
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
		String topic = msg.getTopic();
		String cmd = msg.getCommand();
		res.setId(msg.getId());
		res.setStatus(403);
		res.setTopic(topic);
		
		String text = "403: ";
		if(!StrKit.isEmpty(cmd)){
			text += "Command("+cmd+") ";
		}
		if(!StrKit.isEmpty(topic)){
			text += "Topic(" + topic+") ";
		}
		text += "Forbbiden";
		res.setBody(text); 
		session.write(res);
	} 
	
	public static void reply401(Message msg, Session session, String reason) throws IOException {
		Message res = new Message();
		String topic = msg.getTopic(); 
		res.setId(msg.getId());
		res.setStatus(401);
		res.setTopic(topic);
		 
		res.setBody(reason); 
		session.write(res);
	} 
	
	public static void reply302(Message msg, Session session, String location) throws IOException {
		Message res = new Message(); 
		res.setId(msg.getId());
		res.setStatus(302); 
		res.setHeader("Location", location); 
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
