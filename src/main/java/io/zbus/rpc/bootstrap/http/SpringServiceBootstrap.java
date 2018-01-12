package io.zbus.rpc.bootstrap.http;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.zbus.rpc.Remote;

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
		this.certFile = certFile;
	}  
	
	public void setSslKeyFile(String keyFile){
		this.keyFile = keyFile;
	}   
	 
	 
	public void setServiceToken(String token){  
		serviceToken(token);
	}  
	 
	public void setAutoDiscover(boolean autoDiscover) {
		autoDiscover(autoDiscover);
	}
	
	public void setVerbose(boolean verbose){
		verbose(verbose);
	}
	
	public void setStackTrace(boolean stackTrace){
		stackTrace(stackTrace);
	}
	
	public void setMethodPage(boolean methodPage){
		methodPage(methodPage);
	}
}
