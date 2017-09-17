package io.zbus.examples.rpc.direct;

import io.zbus.examples.rpc.biz.InterfaceExampleImpl;
import io.zbus.examples.rpc.biz.generic.GenericMethodImpl;
import io.zbus.examples.rpc.biz.inheritance.SubService1;
import io.zbus.examples.rpc.biz.inheritance.SubService2;
import io.zbus.rpc.bootstrap.ServiceBootstrap;

public class RpcServiceInproc2 {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		ServiceBootstrap b = new ServiceBootstrap();

		//manually add modules
		b.addModule(InterfaceExampleImpl.class); 
		b.addModule(GenericMethodImpl.class);
		b.addModule(SubService1.class);
		b.addModule(SubService2.class);  
		

		b.serviceName("MyRpc") 
		 .port(15556)    //start server inside
		 .storePath("/tmp/zbus2")
		 //.ssl("ssl/zbus.crt", "ssl/zbus.key") //Enable SSL
		 //.serviceToken("myrpc_service")
		 .start();
	} 
}
