package io.zbus.mq.client;
 
import java.util.concurrent.CountDownLatch;

import io.zbus.mq.api.MqAdmin.Auth;
import io.zbus.mq.api.MqAdmin.Channel;
import io.zbus.mq.api.MqClient;
import io.zbus.mq.api.MqFuture;
import io.zbus.mq.api.MqFutureListener;
import io.zbus.net.IoDriver;
 
public class MqClientTest {
 
	public static void main(String[] args) throws Exception {
		IoDriver ioDriver = new IoDriver();
		
		final MqClient client = new TcpMqClient("localhost:8080", ioDriver);  
		client.configAuth(new Auth());
		 
		long start = System.currentTimeMillis();
		int total = 100000;
		final CountDownLatch count = new CountDownLatch(total);
		
		for(int i=0;i<total;i++){
			MqFuture<Channel> mf = client.declareChannel("MyTopic", "default");
			mf.addListener(new MqFutureListener<Channel>() { 
				@Override
				public void operationComplete(MqFuture<Channel> future) throws Exception {
					count.countDown();
				}
			});
		} 
		
		count.await();
		long end = System.currentTimeMillis();
		System.out.println(total*1000.0/(end-start));
		System.out.println("==done==");
		client.close();
		ioDriver.close();
	} 
}
