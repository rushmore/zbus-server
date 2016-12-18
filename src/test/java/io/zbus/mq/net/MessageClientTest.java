package io.zbus.mq.net;
 
import io.zbus.mq.api.Message;
import io.zbus.net.Future;
import io.zbus.net.FutureListener;
import io.zbus.net.IoDriver;
 
public class MessageClientTest {
 
	public static void main(String[] args) throws Exception {
		IoDriver ioDriver = new IoDriver();
		
		MessageClient client = new MessageClient("localhost:8080", ioDriver);   
		
		for(int i=0; i<10; i++){  
			Message message = new Message();
			message.setCmd("produce");
			message.setTopic("Hong");
			message.setHeader("mq", "hong");
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
