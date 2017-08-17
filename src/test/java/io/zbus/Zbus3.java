package io.zbus;

import io.zbus.mq.server.MqServer;

public class Zbus3 {   
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {     
		new MqServer("conf/zbus3.xml").start();  
	}  
}
