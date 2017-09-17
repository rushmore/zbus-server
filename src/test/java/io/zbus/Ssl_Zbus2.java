package io.zbus;

import io.zbus.mq.server.MqServer;

public class Ssl_Zbus2 {   
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {     
		new MqServer("conf/ssl_zbus2.xml").start();  
	}  
}
