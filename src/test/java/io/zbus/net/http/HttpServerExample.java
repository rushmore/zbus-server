package io.zbus.net.http;

import java.io.IOException;

import io.zbus.transport.Message;
import io.zbus.transport.Server;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session;
import io.zbus.transport.http.HttpWsServer;

public class HttpServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) { 
		
		ServerAdaptor adaptor = new ServerAdaptor() { 
			@Override
			public void onMessage(Object msg, Session sess) throws IOException { 
				Message res = new Message();
				res.setStatus(200);
				
				res.setHeader("id", res.getHeader("id")); 
				res.setHeader("content-type", "text/plain; charset=utf8");
				
				res.setBody("中文"+System.currentTimeMillis());
				
				sess.write(res);  
			}
		};  
		
		Server server = new HttpWsServer();   
		server.start(80, adaptor);  
		//server.start(8080, adaptor); //You may start 80 and 8080 together!
	} 
}
