package io.zbus.mq;
 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.ssl.SslContext;
import io.zbus.kit.ConfigKit;
import io.zbus.mq.MqServerConfig.ServerConfig;
import io.zbus.transport.Ssl;
import io.zbus.transport.http.HttpWsServer; 

public class MqServer extends HttpWsServer {
	private static final Logger logger = LoggerFactory.getLogger(MqServer.class); 
	
	private ServerConfig publicServerConfig; 
	private ServerConfig privateServerConfig;
	private ServerConfig monitorServerConfig;

	private MqServerAdaptor publicServerAdaptor; 
	private MqServerAdaptor privateServerAdaptor; 
	private MonitorServerAdaptor monitorServerAdaptor;
	
	private final MqServerConfig config; 
	
	public MqServer(MqServerConfig config) {  
		this.config = config;
		this.maxSocketCount = config.maxSocketCount;
		
		publicServerConfig = config.publicServer;
		if(publicServerConfig != null) {
			publicServerAdaptor = new MqServerAdaptor(this.config);
			if(publicServerConfig.auth != null) {
				publicServerAdaptor.setRequestAuth(publicServerConfig.auth);
			}
		}
		
		privateServerConfig = config.privateServer;
		if(privateServerConfig != null) {
			if(publicServerAdaptor != null) {
				privateServerAdaptor = publicServerAdaptor.duplicate(); //share internal state
			} else {
				privateServerAdaptor = new MqServerAdaptor(this.config);
			} 
			privateServerAdaptor.setRequestAuth(null);//clear auth by default
			if(privateServerConfig.auth != null) {
				privateServerAdaptor.setRequestAuth(privateServerConfig.auth);
			}
		}  
		
		monitorServerConfig = config.monitorServer;
		if(monitorServerConfig != null) {
			monitorServerAdaptor = new MonitorServerAdaptor(this.config); 
		}
	} 
	public MqServer(String configFile){
		this(new MqServerConfig(configFile));
	}
	
	public MqServerAdaptor getServerAdaptor() {
		return publicServerAdaptor;
	}
	
	public void start() {
		if(publicServerAdaptor != null) { 
			SslContext sslContext = null;
			if (publicServerConfig.sslEnabled){  
				try{  
					sslContext = Ssl.buildServerSsl(publicServerConfig.sslCertFile, publicServerConfig.sslKeyFile); 
				} catch (Exception e) { 
					logger.error("SSL init error: " + e.getMessage());
					throw new IllegalStateException(e.getMessage(), e.getCause());
				} 
			}
			logger.info("Starting public server @" + publicServerConfig.address);
			this.start(publicServerConfig.address, publicServerAdaptor, sslContext); 
		} 
		
		if(privateServerAdaptor != null) { 
			SslContext sslContext = null;
			if (privateServerConfig.sslEnabled){  
				try{  
					sslContext = Ssl.buildServerSsl(privateServerConfig.sslCertFile, privateServerConfig.sslKeyFile); 
				} catch (Exception e) { 
					logger.error("SSL init error: " + e.getMessage());
					throw new IllegalStateException(e.getMessage(), e.getCause());
				} 
			}
			logger.info("Starting private server @" + privateServerConfig.address);
			this.start(privateServerConfig.address, privateServerAdaptor, sslContext); 
		}  
		
		if(monitorServerAdaptor != null) {
			SslContext sslContext = null;
			if (monitorServerConfig.sslEnabled){  
				try{  
					sslContext = Ssl.buildServerSsl(monitorServerConfig.sslCertFile, monitorServerConfig.sslKeyFile); 
				} catch (Exception e) { 
					logger.error("SSL init error: " + e.getMessage());
					throw new IllegalStateException(e.getMessage(), e.getCause());
				} 
			}
			logger.info("Starting mointor server @" + monitorServerConfig.address);
			this.start(monitorServerConfig.address, monitorServerAdaptor, sslContext); 
		} 
	}
	 
	
	public static void main(String[] args) {
		String configFile = ConfigKit.option(args, "-conf", "conf/zbus.xml"); 
		
		final MqServer server;
		try{
			server = new MqServer(configFile);
			server.start(); 
		} catch (Exception e) { 
			e.printStackTrace(System.err);
			logger.warn(e.getMessage(), e); 
			return;
		} 
		
		Runtime.getRuntime().addShutdownHook(new Thread(){ 
			public void run() { 
				try { 
					server.close();
					logger.info("MqServer shutdown completed");
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		});    
	}
}
