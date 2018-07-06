package io.zbus.mq;

import io.zbus.transport.Message;

public class Take {  
	
	public static MqClient buildInproClient() {
		MqServer server = new MqServer(new MqServerConfig());
		return new MqClient(server);
	}
	
	public static void main(String[] args) throws Exception { 
		MqClient client = new MqClient("localhost:15555");
		//MqClient client = buildInproClient();
		
		final String mq = "MyMQ", channel = "MyChannel";
		
		Message req = new Message();
		req.setHeader("cmd", "take");  
		req.setHeader("mq", mq); 
		req.setHeader("channel", channel); 
		
		client.invoke(req, res->{
			System.out.println(res);
			
			client.close();
		}, e->{
			e.printStackTrace();
			try { client.close(); } catch (Exception ex) { }
		});  
	} 
}
