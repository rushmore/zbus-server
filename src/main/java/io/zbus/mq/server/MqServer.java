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
import io.zbus.kit.ConfigKit;
import io.zbus.kit.NetKit;
import io.zbus.kit.StrKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.MessageQueue;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.transport.CodecInitializer;
import io.zbus.transport.ServerAddress;
import io.zbus.transport.Session;
import io.zbus.transport.tcp.TcpServer;

public class MqServer extends TcpServer { 
	private static final Logger log = LoggerFactory.getLogger(MqServer.class); 
	
	private final Map<String, Session> sessionTable = new ConcurrentHashMap<String, Session>();
	private final Map<String, MessageQueue> mqTable = new ConcurrentSkipListMap<String, MessageQueue>(String.CASE_INSENSITIVE_ORDER);
	private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
	
	private MqServerConfig config;   
	private ServerAddress serverAddress;     
	 
	private MqAdaptor mqAdaptor;  
	private Tracker tracker; 
	
	private AtomicLong infoVersion = new AtomicLong(System.currentTimeMillis());
	
	public MqServer(){
		this(new MqServerConfig());
	}
	
	public MqServer(String configFile){
		this(new MqServerConfig(configFile));
	}
	
	public MqServer(MqServerConfig config){  
		super();
		this.config = config.clone();    
		
		codec(new CodecInitializer() {
			@Override
			public void initPipeline(List<ChannelHandler> p) {
				p.add(new HttpServerCodec());
				p.add(new HttpObjectAggregator(loop.getPackageSizeLimit()));
				p.add(new io.zbus.transport.http.MessageCodec());
				p.add(new io.zbus.mq.MessageCodec());
			}
		}); 
		
		if (config.sslEnabled){
			if(!StrKit.isEmpty(config.sslCertFile) && !StrKit.isEmpty(config.sslKeyFile)){  
				try{
					loop.setServerSslContext(config.sslCertFile, config.sslKeyFile);
				} catch (Exception e) {
					e.printStackTrace();
					log.error("SSL disabled: " + e.getMessage());
				}
			} else {
				log.warn("SSL disabled, since SSL certificate file and private file not configured properly");
			}
		}
		
		String host = config.serverHost;
		if("0.0.0.0".equals(host)){
			host = NetKit.getLocalIp();
		}
		String address = host+":"+config.serverPort;
		if(!StrKit.isEmpty(config.serverName)){
			if(config.serverName.contains(":")){
				address = config.serverName; 
			} else {
				address = config.serverName + ":"+config.serverPort; 
			}
		} 
		serverAddress = new ServerAddress(address, loop.isSslEnabled()); 
		
		this.scheduledExecutor.scheduleAtFixedRate(new Runnable() { 
			public void run() {  
				Iterator<Entry<String, MessageQueue>> iter = mqTable.entrySet().iterator();
		    	while(iter.hasNext()){
		    		Entry<String, MessageQueue> e = iter.next();
		    		MessageQueue mq = e.getValue(); 
		    		mq.cleanSession(null); //null to clean all inactive sessions
		    	}
			}
		}, 1000, config.cleanMqInterval, TimeUnit.MILLISECONDS);  
		
		tracker = new Tracker(this, config.sslCertFileTable, 
				!config.trackerOnly, config.trackReportInterval);
		
		mqAdaptor = new MqAdaptor(this);   
	} 
	
	public void start() throws Exception{  
		log.info("Zbus starting...");
		long start = System.currentTimeMillis();  
		this.start(config.serverHost, config.serverPort, mqAdaptor);  
		mqAdaptor.setVerbose(config.verbose);
		try {
			mqAdaptor.loadDiskQueue();
		} catch (IOException e) {
			log.error("Load Message Queue Error: " + e);
		}   
		 
		tracker.joinUpstream(config.getTrackerList());   
		 
		long end = System.currentTimeMillis();
		log.info("Zbus(%s) started sucessfully in %d ms", serverAddress, (end-start)); 
	}
	 
	@Override
	public void close() throws IOException {   
		scheduledExecutor.shutdown();   
		mqAdaptor.close();  
		tracker.close();
		
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
			TopicInfo info = e.getValue().getInfo();
			info.serverAddress = serverAddress;
			table.put(e.getKey(), info);
		}
		ServerInfo info = new ServerInfo(); 
		info.infoVersion = infoVersion.getAndIncrement();
		info.serverAddress = serverAddress;
		info.trackerList = this.tracker.liveTrackerList();
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


