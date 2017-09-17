package io.zbus.examples.rpc.direct;

import io.zbus.rpc.bootstrap.ServiceBootstrap;

public class RpcServiceInproc1 {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		ServiceBootstrap b = new ServiceBootstrap(); 
		
		b.serviceName("MyRpc")
		 .port(15555) // start server inside 
		 .autoDiscover(true)
		 //.ssl("ssl/zbus.crt", "ssl/zbus.key") //Enable SSL
		 //.serviceToken("myrpc_service") //Enable Token authentication
		 .start();
	}
}
