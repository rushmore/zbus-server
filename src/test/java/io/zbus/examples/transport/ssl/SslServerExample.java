package io.zbus.examples.transport.ssl;

import io.zbus.transport.EventLoop;
import io.zbus.transport.http.MessageAdaptor;
import io.zbus.transport.http.MessageServer;

public class SslServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) { 
		EventLoop driver = new EventLoop(); 
		driver.setServerSslContext("ssl/zbus.crt", "ssl/zbus.key");
		
		MessageServer server = new MessageServer(driver); 
		
		server.start(15555, new MessageAdaptor());  
	} 
}
