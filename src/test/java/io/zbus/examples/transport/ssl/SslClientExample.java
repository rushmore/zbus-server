package io.zbus.examples.transport.ssl;
  
import io.zbus.transport.EventLoop;
import io.zbus.transport.http.Message;
import io.zbus.transport.http.MessageClient;

public class SslClientExample {
 
	public static void main(String[] args) throws Exception, InterruptedException { 
		EventLoop driver = new EventLoop(); 
		driver.setClientSslContext("ssl/zbus.crt");   
		
		MessageClient client = new MessageClient("localhost:15555", driver);

		Message req = new Message();
		Message res = client.invokeSync(req);
		
		System.out.println(res);
		
		
		client.close();
		driver.close();
	} 
}
