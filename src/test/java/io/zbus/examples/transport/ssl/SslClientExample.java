package io.zbus.examples.transport.ssl;
  
import io.zbus.transport.EventLoop;
import io.zbus.transport.ServerAddress;
import io.zbus.transport.http.Message;
import io.zbus.transport.http.MessageClient;

public class SslClientExample {
 
	public static void main(String[] args) throws Exception, InterruptedException { 
		EventLoop loop = new EventLoop();  
		
		ServerAddress serverAddress = new ServerAddress();
		serverAddress.setAddress("localhost:15555");
		serverAddress.setSslEnabled(true);
		serverAddress.setCertFile("ssl/zbus.crt");
		
		
		MessageClient client = new MessageClient(serverAddress, loop);

		Message req = new Message();
		Message res = client.invokeSync(req);
		
		System.out.println(res);
		
		
		client.close();
		loop.close();
	} 
}
