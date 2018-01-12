package io.zbus.examples.rpc;

import io.zbus.rpc.bootstrap.mq.ServiceBootstrap;

/**
 * Will start zbus server internally
 * 
 * @author Rushmore
 *
 */
public class RpcService {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		ServiceBootstrap b = new ServiceBootstrap(); 
		
		//manually add modules
		//b.addModule(InterfaceExampleImpl.class); 
		//b.addModule(GenericMethodImpl.class);
		//b.addModule(SubService1.class);
		//b.addModule("index", Index.class);  
		
		b.serviceName("MyRpc") // application level entry, full URL: <service> / <module> / <method> 
		 .port(15555)          // start server inside 
		 //.serviceAddress("localhost:15555;localhost:15556")  //Connect to remote trackers
		 .autoDiscover(true)   // disable if add modules manually
		 .connectionCount(4)   // parallel connection count
		 //.serviceMask(Protocol.MASK_DISK)
		 .responseTypeInfo(false) //Enable it if Generic method proxy required!!!
		 .verbose(true)
		 .stackTrace(false)    // enable stackTrace info to client if true
		 .methodPage(true)     // Show method page if true 
		 //.ssl("ssl/zbus.crt", "ssl/zbus.key") //Enable SSL
		 //.serviceToken("myrpc_service") //Enable Token authentication
		 .start();
	}
}
