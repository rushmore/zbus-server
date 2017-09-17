package io.zbus.rpc.bootstrap;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.zbus.rpc.Remote;
import io.zbus.transport.ServerAddress;

public class SpringServiceBootstrap extends ServiceBootstrap implements ApplicationContextAware {
	private ApplicationContext context;
	
	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException { 
		this.context = context;
	}

	@Override
	protected void initProcessor() { 
		Map<String, Object> table = context.getBeansWithAnnotation(Remote.class);
		for(Object remoteInstance : table.values()){
			processor.addModule(remoteInstance);
		}
	} 
	
	public void setModuleList(List<Object> instances){
		if(instances == null) return;
		for(Object instance : instances){
			processor.addModule(instance);
		}
	}
	
	public void setModuleTable(Map<String, Object> instances){
		if(instances == null) return;
		for(Entry<String, Object> e : instances.entrySet()){
			processor.addModule(e.getKey(), e.getValue());
		}
	}
	
	public void setPort(int port){
		port(port);
	} 
	 
	public void setHost(String host){
		host(host); 
	}   
	
	public void setSslCertFile(String certFile){
		ssl(certFile, null);
	}  
	
	public void setSslKeyFile(String keyFile){
		ssl(null, keyFile);
	}   
	
	public void setStorePath(String mqPath){
		storePath(mqPath);
	}   
	
	public void setServiceAddress(ServerAddress... tracker){
		serviceAddress(tracker);
	}
	
	public void setServiceAddress(String tracker){
		serviceAddress(tracker);
	} 
	
	public void setServiceName(String topic){
		serviceName(topic);
	}
	
	public void setServiceMask(int mask){
		serviceMask(mask);
	}
	
	public void setServiceToken(String token){  
		serviceToken(token);
	} 
	
	public void setConnectionCount(int connectionCount){ 
		connectionCount(connectionCount);
	} 
	 
	public void setAutoDiscover(boolean autoDiscover) {
		autoDiscover(autoDiscover);
	}
}
