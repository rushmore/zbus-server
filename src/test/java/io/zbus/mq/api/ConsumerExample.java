package io.zbus.mq.api;

import io.zbus.mq.api.MqAdmin.Auth;
import io.zbus.mq.client.TcpMqClient;
import io.zbus.net.IoDriver;

public class ConsumerExample {

	public static void main(String[] args) { 
		IoDriver ioDriver = new IoDriver();
		
		@SuppressWarnings("resource")
		final MqClient client = new TcpMqClient("localhost:8080", ioDriver);  
		client.configAuth(new Auth()); 
		 
	}

}
