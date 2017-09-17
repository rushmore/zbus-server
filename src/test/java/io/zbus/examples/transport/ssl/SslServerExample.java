package io.zbus.examples.transport.ssl;

import io.netty.handler.ssl.SslContext;
import io.zbus.transport.EventLoop;
import io.zbus.transport.SslKit;
import io.zbus.transport.http.MessageAdaptor;
import io.zbus.transport.http.MessageServer;

public class SslServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) { 
		
		SslContext context = SslKit.buildServerSsl("ssl/zbus.crt", "ssl/zbus.key");
		
		EventLoop loop = new EventLoop(); 
		loop.setSslContext(context);
		
		MessageServer server = new MessageServer(loop);  
		server.start(15555, new MessageAdaptor());  
	} 
}
