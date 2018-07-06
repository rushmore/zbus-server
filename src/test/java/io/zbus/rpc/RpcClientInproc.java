package io.zbus.rpc;

import java.util.concurrent.atomic.AtomicInteger;

import io.zbus.rpc.biz.InterfaceExampleImpl;
import io.zbus.transport.Message;
import io.zbus.transport.inproc.InprocClient;

public class RpcClientInproc {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		RpcProcessor p = new RpcProcessor();
		p.mount("example", InterfaceExampleImpl.class);
		
		RpcServer server = new RpcServer(p);    
		server.start(); 
		
		InprocClient rpc = new InprocClient(server.getServerAdaptor());
		
		AtomicInteger count = new AtomicInteger(0);  
		for (int i = 0; i < 1000000; i++) {
			Message req = new Message();
			req.setUrl("/example/getOrder"); 
			
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
