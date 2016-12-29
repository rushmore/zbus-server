package io.zbus.mq.client;
 
import java.util.concurrent.TimeUnit;

import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.ScheduledFuture;
import io.zbus.net.IoDriver;
 
public class TimerTest {
 
	public static void main(String[] args) throws Exception {
		IoDriver ioDriver = new IoDriver();
		
		EventLoopGroup loop = ioDriver.getGroup();
		ScheduledFuture<?> sch = loop.scheduleAtFixedRate(new Runnable() {
			
			@Override
			public void run() {  
				System.out.println("on timer"); 
			}
		}, 0, 1, TimeUnit.SECONDS);
	 
		Thread.sleep(4000);
		sch.cancel(false);
		System.out.println("done");
		ioDriver.close();
		System.out.println("done");
	} 
}
