package io.zbus.net.ssl;

import java.io.IOException;

import io.netty.handler.ssl.SslContext;
import io.zbus.transport.Message;
import io.zbus.transport.Server;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session;
import io.zbus.transport.Ssl;
import io.zbus.transport.http.HttpWsServer; 

public class SslServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) { 
		
		SslContext context = Ssl.buildServerSsl("ssl/zbus.crt", "ssl/zbus.key"); 
		
		Server server = new HttpWsServer();   
		ServerAdaptor adaptor = new ServerAdaptor() { 
			@Override
			public void onMessage(Object msg, Session sess) throws IOException { 
				Message res = new Message();
				res.setStatus(200);
				res.setHeader("content-type", "text/html; charset=utf8"); 
				res.setBody("<h1>hello world</h1>");  
				
				sess.write(res);
			}
		}; 
		server.start(8080, adaptor, context);  
	} 
}
