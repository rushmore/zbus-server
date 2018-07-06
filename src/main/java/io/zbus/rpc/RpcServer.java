package io.zbus.rpc;

import java.io.Closeable;
import java.io.IOException;

import io.netty.handler.ssl.SslContext;
import io.zbus.mq.MqServer;
import io.zbus.rpc.server.HttpRpcServerAdaptor;
import io.zbus.rpc.server.MqRpcServer;
import io.zbus.transport.IoAdaptor;
import io.zbus.transport.Ssl;
import io.zbus.transport.http.HttpWsServer; 
 
/**
 * 
 * Notes on authentication:
 * 1) HTTP(WS) direct RPC, authentication through RpcProcessor's AuthFilter
 * 2) MQ based RPC, authentication through MQ
 * 
 * These two authentications should not mix together, i.e, MQ based RPC should remove AuthFilter in RpcProcessor
 * 
 * @author leiming.hong Jun 30, 2018
 *
 */
public class RpcServer implements Closeable {   
	private RpcProcessor processor;   
	private RpcStartInterceptor onStart; 
	
	//RPC over HTTP/WS
	private Integer port;
	private String host = "0.0.0.0"; 
	private String certFile;
	private String keyFile;
	
	private HttpWsServer httpWsServer; 
	private HttpRpcServerAdaptor httpRpcServerAdaptor; 
	
	//RPC over MQ
	private String mq;
	private String mqType;
	private String channel;         //Default to MQ
	private MqServer mqServer;      //InProc MQ server
	private String mqServerAddress; //Support MQ based RPC
	private Integer mqClientCount;
	private Integer mqHeartbeatInterval;
	private MqRpcServer mqRpcServer;
	
	//Auth for mq client
	private boolean authEnabled = false; //Enable mq channel's authentication
	private String apiKey;
	private String secretKey;
	
	public RpcServer() { 
	} 
	 
	public RpcServer(RpcProcessor processor) {
		this.processor = processor; 
	} 
	
	public RpcProcessor getProcessor() {
		return processor;
	}
	
	public void setProcessor(RpcProcessor processor) {
		this.processor = processor;
		
		if(this.httpRpcServerAdaptor != null) {
			this.httpRpcServerAdaptor.setProcessor(processor);
		} 
		if(mqRpcServer != null) {
			mqRpcServer.setProcessor(processor);
		}
	}
	
	public RpcServer setPort(Integer port){ 
		this.port = port;
		return this;
	} 
	 
	public RpcServer setHost(String host){ 
		this.host = host;
		return this;
	}    
	
	public RpcServer setMq(String mq){ 
		this.mq = mq; 
		return this;
	}    
	
	public RpcServer setMqType(String mqType){ 
		this.mqType = mqType;
		return this;
	}    
	
	public RpcServer setMqClientCount(Integer count){ 
		this.mqClientCount = count;
		return this;
	}    
	
	public RpcServer setMqHeartbeatInterval(Integer mqHeartbeatInterval) {
		this.mqHeartbeatInterval = mqHeartbeatInterval;
		return this;
	}
	
	public RpcServer setChannel(String channel) {
		this.channel = channel;
		return this;
	} 
	/**
	 * Set InProc server instance as address
	 * @param mqServer
	 * @return
	 */
	public RpcServer setMqServer(MqServer mqServer){  
		this.mqServer = mqServer;
		return this;
	}  
	
	public RpcServer setCertFile(String certFile){ 
		this.certFile = certFile; 
		return this;
	}  
	
	public RpcServer setKeyFile(String keyFile){ 
		this.keyFile = keyFile;
		return this;
	}   
	   
	public void setAuthEnabled(boolean mqAuthEnabled) {
		this.authEnabled = mqAuthEnabled;
	}
	public void setApiKey(String mqApiKey) {
		this.apiKey = mqApiKey;
	}
	public void setSecretKey(String mqSecretKey) {
		this.secretKey = mqSecretKey;
	}
	
	public IoAdaptor getServerAdaptor() { 
		if(mqRpcServer != null) {
			MqServer mqServer = mqRpcServer.getMqServer();
			if(mqServer != null) {
				return mqServer.getServerAdaptor();
			} else {
				throw new IllegalStateException("MqServer not started as inproce mode");
			}
		}
		if(httpRpcServerAdaptor == null) {
			this.httpRpcServerAdaptor = new HttpRpcServerAdaptor(processor);
		}
		return httpRpcServerAdaptor;
	}

	public void setMqServerAddress(String mqServerAddress) {
		this.mqServerAddress = mqServerAddress;
	}

	public void setOnStart(RpcStartInterceptor onStart) {
		this.onStart = onStart;
	} 
	 
	public RpcServer start() throws Exception{  
		if(this.processor == null) {
			throw new IllegalStateException("Missing RpcProcessor");
		}
		if(onStart != null) {
			onStart.onStart(processor);
		} 
		
		if(port != null) {
			this.httpWsServer = new HttpWsServer();  
			this.httpRpcServerAdaptor = new HttpRpcServerAdaptor(processor);
			SslContext context = null;
			if(keyFile != null && certFile != null) {
				context = Ssl.buildServerSsl(certFile, keyFile); 
			}   
			httpWsServer.start(String.format("%s:%d",this.host, this.port), httpRpcServerAdaptor, context); 
		} 
		
		if(mq != null) {   
			this.mqRpcServer = new MqRpcServer(this.processor);
			if(this.mqServer != null) {
				if(!mqServer.isStarted()) {
					mqServer.start();
				}
				mqRpcServer.setMqServer(this.mqServer);
			} else if(this.mqServerAddress != null) {
				mqRpcServer.setAddress(mqServerAddress);
			}
			mqRpcServer.setMq(this.mq);
			mqRpcServer.setAuthEnabled(authEnabled);
			mqRpcServer.setApiKey(apiKey);
			mqRpcServer.setSecretKey(secretKey);
			
			if(this.mqType != null) {
				mqRpcServer.setMqType(mqType);
			}
			if(this.channel != null) {
				mqRpcServer.setChannel(this.channel);
			}
			if(this.mqClientCount != null) {
				mqRpcServer.setClientCount(mqClientCount);
			}
			if(this.mqHeartbeatInterval != null) {
				mqRpcServer.setHeartbeatInterval(mqHeartbeatInterval);
			}
			
			mqRpcServer.start();
		} 
		
		//Doc URL root generated
		if(processor.isDocEnabled()) {
			processor.mountDoc();
		}
		
		return this;
	}     
	
	public void syncUrlToServer() {
		if(mqRpcServer != null) {
			mqRpcServer.syncUrlToServer();
		}
	}
	
	@Override
	public void close() throws IOException {  
		if(httpWsServer != null) {
			httpWsServer.close();
			httpWsServer = null;
		} 
		if(mqRpcServer != null) {
			mqRpcServer.close();
			mqRpcServer = null;
		}
	}   
}
