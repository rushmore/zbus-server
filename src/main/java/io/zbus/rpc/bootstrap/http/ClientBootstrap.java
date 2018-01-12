package io.zbus.rpc.bootstrap.http;

import java.io.Closeable;
import java.io.IOException;

import io.zbus.rpc.RpcConfig;
import io.zbus.rpc.RpcInvoker;
import io.zbus.rpc.transport.http.RpcMessageInvoker;
import io.zbus.transport.ServerAddress;
import io.zbus.transport.http.MessageClientPool;

public class ClientBootstrap implements Closeable {   
	protected MessageClientPool clientPool;
	protected String token;
	protected ServerAddress serverAddress;
	protected int clientPoolSize = 32; 
	protected RpcConfig rpcConfig = new RpcConfig(); 
	
	protected boolean requestTypeInfo = true; 
	
	public ClientBootstrap requestTypeInfo(boolean requestTypeInfo){  
		this.requestTypeInfo = requestTypeInfo;
		return this;
	}    
	
	public ClientBootstrap serviceToken(String token){  
		this.token = token;
		return this;
	}  
	
	public ClientBootstrap serviceAddress(ServerAddress serverAddress){
		this.serverAddress = serverAddress;
		return this;
	}
	
	public ClientBootstrap serviceAddress(String serverAddress){
		this.serverAddress = new ServerAddress(serverAddress);
		return this;
	}
	
	public ClientBootstrap clientPoolSize(int clientPoolSize){
		this.clientPoolSize = clientPoolSize;
		return this;
	}
	
	public RpcInvoker invoker(){
		if(clientPool == null){
			 clientPool = new MessageClientPool(serverAddress, clientPoolSize, null);
		} 
		RpcMessageInvoker messageInvoker = new RpcMessageInvoker(clientPool);
		messageInvoker.setToken(this.token);
		rpcConfig.setMessageInvoker(messageInvoker);
		RpcInvoker rpcInvoker = new RpcInvoker(rpcConfig); 
		rpcInvoker.getCodec().setRequestTypeInfo(requestTypeInfo); 
		
		return rpcInvoker;
	}
	
	public <T> T createProxy(Class<T> clazz){  
		return invoker().createProxy(clazz); 
	}     
	
	@Override
	public void close() throws IOException { 
		if(clientPool != null){
			clientPool.close();
			clientPool = null;
		}
	} 
}
