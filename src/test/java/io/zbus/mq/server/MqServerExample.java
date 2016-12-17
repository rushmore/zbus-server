package io.zbus.mq.server;

public class MqServerExample {
	
	public static void main(String[] args) throws Exception {    
		MqServer server = new MqServer();
		
		try {
			server.start(8080); 
			server.join();
		} finally { 
			server.close();
		} 
	} 
}
