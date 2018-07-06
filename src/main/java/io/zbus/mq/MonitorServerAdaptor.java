package io.zbus.mq;

import io.zbus.kit.FileKit;
import io.zbus.rpc.RpcAuthFilter;
import io.zbus.rpc.RpcProcessor;
import io.zbus.rpc.StaticResource;
import io.zbus.rpc.annotation.RequestMapping;
import io.zbus.rpc.server.HttpRpcServerAdaptor;
import io.zbus.transport.Message;

public class MonitorServerAdaptor extends HttpRpcServerAdaptor {  
	public MonitorServerAdaptor(MqServerConfig config) {
		 super(new RpcProcessor());
		 if(config.monitorServer != null && config.monitorServer.auth != null) {
			 RpcAuthFilter authFilter = new RpcAuthFilter(config.monitorServer.auth);
			 processor.setAuthFilter(authFilter);
		 }
		 processor.mount("", new MonitorService());
		 StaticResource staticResource = new StaticResource();
		 staticResource.setCacheEnabled(false); //TODO turn if off in production
		 processor.mount("static", staticResource, false);
	} 
}
 

class MonitorService {  
	private FileKit fileKit = new FileKit();
	
	@RequestMapping("/")
	public Message home() { 
		return fileKit.loadResource("static/home.htm");
	} 
	
	@RequestMapping(path="/favicon.ico", docEnabled=false)
	public Message favicon() { 
		return fileKit.loadResource("static/favicon.ico"); 
	}
}

