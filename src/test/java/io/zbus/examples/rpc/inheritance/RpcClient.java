package io.zbus.examples.rpc.inheritance;

import io.zbus.mq.Broker;
import io.zbus.rpc.RpcInvoker;

public class RpcClient {

	public static void main(String[] args) throws Exception { 
		Broker broker = new Broker("localhost:15555");    
	
		RpcInvoker rpc = new RpcInvoker(broker, "Inherit");
		
		SubServiceInterface1 service1 = rpc.createProxy(SubServiceInterface1.class);
		SubServiceInterface2 service2 = rpc.createProxy(SubServiceInterface2.class);
		
		service1.save(new Integer(10));
		service2.save("test");
		
		broker.close(); 
	}

}
