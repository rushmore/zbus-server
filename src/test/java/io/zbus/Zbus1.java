package io.zbus;

import io.zbus.mq.server.MqServer;

public class Zbus1 {   
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {     
		new MqServer("conf/zbus1.xml").start();  
	}  
}
