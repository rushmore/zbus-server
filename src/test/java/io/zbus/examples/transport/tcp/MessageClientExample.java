package io.zbus.examples.transport.tcp;
  
import io.zbus.transport.EventLoop;
import io.zbus.transport.http.Message;
import io.zbus.transport.http.MessageClient;

public class MessageClientExample {
 
	public static void main(String[] args) throws Exception, InterruptedException { 
		EventLoop loop = new EventLoop();  
		MessageClient client = new MessageClient("localhost", loop);

		Message req = new Message(); 
		req.setBody("中文");
		Message res = client.invokeSync(req);
		
		System.out.println(res); 
		
		client.close();
		loop.close();
	} 
}
