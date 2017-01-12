package io.zbus.mq.net;
 
import io.zbus.net.Future;
import io.zbus.net.FutureListener;
import io.zbus.mq.Message;
import io.zbus.net.EventDriver;
 
public class MessageClientTest {
 
	public static void main(String[] args) throws Exception {
		EventDriver ioDriver = new EventDriver();
		
		MessageClient client = new MessageClient("localhost:8080", ioDriver);   
		
		for(int i=0; i<10; i++){  
			Message message = new Message();
			message.setCmd("produce");
			message.setMq("Hong"); 
			Future<Message> res = client.invoke(message);    
			
			res.addListener(new FutureListener<Message>() {
				
				@Override
				public void operationComplete(Future<Message> future) throws Exception {
					System.out.println(future.get());
				}
			});
		}
		
		System.out.println("===done===");
		client.close();
		//ioDriver.close();
	} 
}
