package io.zbus.examples.transport.tcp;

import org.junit.Test;

import io.zbus.transport.http.MessageAdaptor;
import io.zbus.transport.http.MessageServer;

public class ServerTest {
 
	@Test
	public void testServerClose() throws Exception{
		MessageServer server = new MessageServer();   
		server.start(80, new MessageAdaptor());   
		Thread.sleep(100);
		server.close();
	} 
	
	public static void main(String[] args) throws Exception {   
		MessageServer server = new MessageServer();   
		server.start(80, new MessageAdaptor());   
		Thread.sleep(100);
		server.close();
	} 
}
