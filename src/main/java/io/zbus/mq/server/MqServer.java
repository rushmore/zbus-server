package io.zbus.mq.server;
 

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.zbus.kit.ConfigKit;
import io.zbus.kit.FileKit;
import io.zbus.kit.NetKit;
import io.zbus.kit.StrKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Broker;
import io.zbus.mq.MessageQueue;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.proxy.http.HttpProxy;
import io.zbus.proxy.http.ProxyConfig;
import io.zbus.transport.CodecInitializer;
import io.zbus.transport.IoAdaptor;
import io.zbus.transport.ServerAddress;
import io.zbus.transport.Session;
import io.zbus.transport.SslKit;
import io.zbus.transport.tcp.TcpServer;

public class MqServer extends TcpServer { 
	private static final Logger log = LoggerFactory.getLogger(MqServer.class); 
	
	private final Map<String, Session> sessionTable = new ConcurrentHashMap<String, Session>();
	private final Map<String, MessageQueue> mqTable = new ConcurrentSkipListMap<String, MessageQueue>(String.CASE_INSENSITIVE_ORDER);
	final Map<String, String> sslCertTable = new ConcurrentHashMap<String, String>(); 
	
	private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
	
	private MqServerConfig config;   
	private ServerAddress serverAddress;     
	 
	private MqAdaptor mqAdaptor;  
	private Tracker tracker; 
	private HttpProxy httpProxy;
	
	private AtomicLong infoVersion = new AtomicLong(System.currentTimeMillis());
	
	public MqServer(){
		this(new MqServerConfig());
	}
	
	public MqServer(String configFile){
		this(new MqServerConfig(configFile));
	}
	
	public MqServer(MqServerConfig serverConfig){   
		config = serverConfig.clone(); 
		
		Fix.Enabled = config.isCompatible();
		
		codec(new CodecInitializer() {
			@Override
			public void initPipeline(List<ChannelHandler> p) {
				p.add(new HttpServerCodec());
				p.add(new HttpObjectAggregator(loop.getPackageSizeLimit()));
				p.add(new io.zbus.transport.http.MessageCodec());
				p.add(new io.zbus.mq.MessageCodec());
			}
		}); 
		
		boolean sslEnabled = config.isSslEnabled();
		String certFileContent = "";
		if (sslEnabled){  
			try{ 
				certFileContent = FileKit.renderFile(config.getSslCertFile());
				SslContext sslContext = SslKit.buildServerSsl(config.getSslCertFile(), config.getSslKeyFile());
				loop.setSslContext(sslContext); 
			} catch (Exception e) { 
				log.error("SSL init error: " + e.getMessage());
				throw new IllegalStateException(e.getMessage(), e.getCause());
			} 
		}
		
		String host = config.getServerHost();
		if("0.0.0.0".equals(host)){
			host = NetKit.getLocalIp();
		}
		String address = host+":"+config.getServerPort();
		String serverName = config.getServerName();
		if(!StrKit.isEmpty(serverName)){
			if(serverName.contains(":")){
				address = serverName; 
			} else {
				address = serverName + ":"+config.getServerPort(); 
			}
		} 
		serverAddress = new ServerAddress(address, sslEnabled); 
		if(sslEnabled) { //Add current server's SSL certificate file to table
			serverAddress.setCertificate(certFileContent);
			sslCertTable.put(serverAddress.getAddress(), certFileContent);
		}
		
		this.scheduledExecutor.scheduleAtFixedRate(new Runnable() { 
			public void run() {  
				Iterator<Entry<String, MessageQueue>> iter = mqTable.entrySet().iterator();
		    	while(iter.hasNext()){
		    		Entry<String, MessageQueue> e = iter.next();
		    		MessageQueue mq = e.getValue(); 
		    		mq.cleanSession(null); //null to clean all inactive sessions
		    	}
			}
		}, 1000, config.getCleanMqInterval(), TimeUnit.MILLISECONDS);   
		
		tracker = new Tracker(this);
		
		//adaptor needs tracker built first
		mqAdaptor = new MqAdaptor(this); 
		mqAdaptor.setVerbose(config.isVerbose()); 
		try {
			mqAdaptor.loadDiskQueue();
		} catch (IOException e) {
			log.error("Load Message Queue Error: " + e);
		}   
		
		loadHttpProxy(config.getHttpProxyConfig());
	} 
	
	private void loadHttpProxy(ProxyConfig config){ 
		if(config == null || config.getEntryTable().isEmpty()) return; 
		
		try {
			Broker broker = new Broker(this);
			config.setBroker(broker); //InProc broker
			httpProxy = new HttpProxy(config);
			httpProxy.start();
		} catch (IOException e) {
			log.error(e.getMessage(), e.getCause());
		}
	}
	
	@Override
	public IoAdaptor getIoAdaptor() {
		return this.mqAdaptor;
	}
	
	public void start() throws Exception{  
		log.info("Zbus starting...");
		long start = System.currentTimeMillis();  
		this.start(config.getServerHost(), config.getServerPort(), mqAdaptor);   
		 
		tracker.joinTracker(config.getTrackerList());   
		 
		long end = System.currentTimeMillis();
		log.info("Zbus(%s) started sucessfully in %d ms", serverAddress, (end-start)); 
	}
	 
	@Override
	public void close() throws IOException {   
		scheduledExecutor.shutdown();   
		mqAdaptor.close();  
		tracker.close();
		if(httpProxy != null){
			httpProxy.close();
		}
		
		super.close();
	}  
    
	public Map<String, MessageQueue> getMqTable() {
		return mqTable;
	} 

	public Map<String, Session> getSessionTable() {
		return sessionTable;
	}  

	public ServerAddress getServerAddress() {
		return serverAddress;
	}  

	public MqServerConfig getConfig() {
		return config;
	}    

	public Tracker getTracker() {
		return tracker;
	} 
	
	public ServerInfo serverInfo() {
		Map<String, TopicInfo> table = new HashMap<String, TopicInfo>();
		for (Map.Entry<String, MessageQueue> e : this.mqTable.entrySet()) {
			TopicInfo info = e.getValue().topicInfo();
			info.serverAddress = serverAddress;
			table.put(e.getKey(), info);
		}
		ServerInfo info = new ServerInfo(); 
		info.infoVersion = infoVersion.getAndIncrement();
		info.serverAddress = serverAddress;
		info.trackerList = this.tracker.trackerList();
		info.topicTable = table; 
 
		return info;
	}

	public static void main(String[] args) throws Exception { 
		String configFile = ConfigKit.option(args, "-conf", "conf/zbus.xml"); 
		
		final MqServer server;
		try{
			server = new MqServer(configFile);
			server.start(); 
		} catch (Exception e) { 
			e.printStackTrace(System.err);
			log.warn(e.getMessage(), e); 
			return;
		} 
		
		Runtime.getRuntime().addShutdownHook(new Thread(){ 
			public void run() { 
				try { 
					server.close();
					log.info("MqServer shutdown completed");
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		});    
	} 
}


