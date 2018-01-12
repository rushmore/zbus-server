package io.zbus.rpc.bootstrap.http;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

import io.netty.handler.ssl.SslContext;
import io.zbus.kit.ClassKit;
import io.zbus.rpc.Remote;
import io.zbus.rpc.RpcProcessor;
import io.zbus.rpc.transport.http.RpcMessageHandler;
import io.zbus.transport.EventLoop;
import io.zbus.transport.SslKit;
import io.zbus.transport.http.MessageServer;
 

public class ServiceBootstrap implements Closeable {  
	protected RpcProcessor processor = new RpcProcessor(); 
	protected boolean autoDiscover = false;
	protected int port;
	protected String host = "0.0.0.0";
	protected String certFile;
	protected String keyFile;
	protected MessageServer server; 
	protected String token;
	protected EventLoop eventLoop;
	  
	protected boolean verbose = false;   
	
	
	public ServiceBootstrap port(int port){ 
		this.port = port;
		return this;
	} 
	 
	public ServiceBootstrap host(String host){ 
		this.host = host;
		return this;
	}   
	
	public ServiceBootstrap ssl(String certFile, String keyFile){ 
		this.certFile = certFile;
		this.keyFile = keyFile;
		return this;
	}  
	 
	public ServiceBootstrap autoDiscover(boolean autoDiscover) {
		this.autoDiscover = autoDiscover;
		return this;
	} 
	
	public ServiceBootstrap verbose(boolean verbose) {
		this.verbose = verbose;
		return this;
	} 
	
	public ServiceBootstrap responseTypeInfo(boolean responseTypeInfo){  
		processor.getCodec().setResponseTypeInfo(responseTypeInfo);
		return this;
	}   
	
	public ServiceBootstrap stackTrace(boolean stackTrace) {
		this.processor.setEnableStackTrace(stackTrace);
		return this;
	} 
	
	public ServiceBootstrap methodPage(boolean methodPage) {
		this.processor.setEnableMethodPage(methodPage);
		return this;
	} 
	
	public ServiceBootstrap serviceToken(String token){ 
		this.token = token;
		return this;
	}  
	
	private void validate(){ 
		
	}
	
	protected void initProcessor(){  
		Set<Class<?>> classes = ClassKit.scan(Remote.class);
		for(Class<?> clazz : classes){
			processor.addModule(clazz);
		}  
	}
	 
	public ServiceBootstrap start() throws Exception{
		validate();  
		
		if(autoDiscover){
			initProcessor();
		} 
		eventLoop = new EventLoop();
		if(keyFile != null && certFile != null) {
			SslContext context = SslKit.buildServerSsl(certFile, keyFile);
			eventLoop.setSslContext(context);
		}
		
		server = new MessageServer(eventLoop);    
		RpcMessageHandler adaptor = new RpcMessageHandler(this.processor);
		adaptor.setToken(this.token);
		adaptor.setVerbose(this.verbose);
		server.start(this.host, this.port, adaptor); 
		
		return this;
	}  
	
	public ServiceBootstrap addModule(Class<?>... clazz){
		processor.addModule(clazz);
		return this;
	}  
	
	public ServiceBootstrap addModule(String module, Class<?>... clazz){
		processor.addModule(module, clazz);
		return this;
	}
	
	public ServiceBootstrap addModule(String module, Object... services){
		processor.addModule(module, services);
		return this;
	}
	
	public ServiceBootstrap addModule(Object... services){
		processor.addModule(services);
		return this;
	}
	
	
	@Override
	public void close() throws IOException {  
		if(server != null) {
			server.close();
		}
		if(eventLoop != null) {
			eventLoop.close();
		}
	}   
}
