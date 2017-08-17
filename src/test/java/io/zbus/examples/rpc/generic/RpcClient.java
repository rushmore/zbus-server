package io.zbus.examples.rpc.generic;

import io.zbus.mq.Broker;
import io.zbus.rpc.RpcInvoker;

public class RpcClient {

	public static void main(String[] args) throws Exception { 
		Broker broker = new Broker("localhost:15555");    
	
		RpcInvoker rpc = new RpcInvoker(broker, "Generic");
		
		GenericMethod m = rpc.createProxy(GenericMethod.class);  
		m.test(10);
		
		broker.close(); 
	}

}
