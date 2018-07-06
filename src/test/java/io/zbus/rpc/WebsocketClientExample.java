package io.zbus.rpc;

import java.util.concurrent.atomic.AtomicInteger;

import io.zbus.transport.Message;
import io.zbus.transport.http.WebsocketClient;

public class WebsocketClientExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		WebsocketClient rpc = new WebsocketClient("localhost:8080");
		
		AtomicInteger count = new AtomicInteger(0);  
		for (int i = 0; i < 100000; i++) {
			Message req = new Message();
			req.setHeader("method", "getOrder");
			req.setHeader("module", "example");
			 
			
			rpc.invoke(req, res->{
				int c = count.getAndIncrement();
				if(c % 10000 == 0) {
					System.out.println(c + ": " + res);
				}
			});
		}
		//rpc.close();
	}
}
