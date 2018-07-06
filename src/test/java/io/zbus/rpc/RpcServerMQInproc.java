package io.zbus.rpc;

import io.zbus.mq.MqServer;
import io.zbus.mq.MqServerConfig;
import io.zbus.rpc.biz.InterfaceExampleImpl;

public class RpcServerMQInproc {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		  
		RpcProcessor p = new RpcProcessor();
		p.setUrlPrefix("/");  
		p.mount("/example", InterfaceExampleImpl.class);  
		
		
		//Serve RPC via MQ Server InProc
		MqServerConfig config = new MqServerConfig("0.0.0.0", 15555);  
		config.setVerbose(false);
		MqServer mqServer = new MqServer(config);  
		
		RpcServer server = new RpcServer(p);   
		server.setMqServer(mqServer); //InProc MqServer
		server.setMq("MyRpc");        //Choose MQ to group Service physically, RPC incognito
		
		//MQ authentication, no need to configure if use HTTP direct RPC
		//server.setAuthEnabled(true);
		//server.setApiKey("2ba912a8-4a8d-49d2-1a22-198fd285cb06");
		//server.setSecretKey("461277322-943d-4b2f-b9b6-3f860d746ffd"); 
		
		server.start();
	} 
}
