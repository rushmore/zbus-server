package io.zbus.net.ws;

import java.util.HashMap;
import java.util.Map;

import io.zbus.transport.Message;
import io.zbus.transport.http.WebsocketClient;

public class WebSocketExample {
 
	public static void main(String[] args) throws Exception {  
		
		WebsocketClient ws = new WebsocketClient("wss://zbus.io"); 
		ws.onText = data -> {
			 System.out.println(data);
			 ws.close();
		};   
		
		ws.onOpen(()->{
			Map<String, Object> command = new HashMap<>();
			command.put("module", "example");
			command.put("method", "echo");
			command.put("params", new Object[] {"hong"});
			
			Message message = new Message();
			message.setBody(command);
			
			//for MQ
			message.setHeader("cmd", "pub");
			message.setHeader("mq", "MyMQ");
			message.setHeader("ack", false); 
			
			ws.sendMessage(message);
			
		});
		
		ws.connect();  
	}
}
