package io.zbus.mq.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.zbus.kit.JsonKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Message;
import io.zbus.mq.MqClient;
import io.zbus.mq.Protocol;
import io.zbus.mq.Protocol.ServerEvent;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TrackerInfo;
import io.zbus.transport.EventLoop;
import io.zbus.transport.ServerAddress;
import io.zbus.transport.Session;
import io.zbus.transport.Client.ConnectedHandler;
import io.zbus.transport.Client.DisconnectedHandler; 
 

public class Tracker implements Closeable{
	private static final Logger log = LoggerFactory.getLogger(Tracker.class);  
	 
	private Map<ServerAddress, MqClient> downstreamTrackers = new ConcurrentHashMap<ServerAddress, MqClient>();  
	private Map<ServerAddress, MqClient> healthyUpstreamTrackers = new ConcurrentHashMap<ServerAddress, MqClient>();
	private Map<ServerAddress, MqClient> upstreamTrackers = new ConcurrentHashMap<ServerAddress, MqClient>();  
	
	private Map<String, ServerInfo> serverTable = new ConcurrentHashMap<String, ServerInfo>();  
	
	private Set<Session> subscribers = new HashSet<Session>();
	
	private MqServer mqServer;
	private EventLoop loop; 
	private final ServerAddress myServerAddress;
	private boolean myServerInTrack; 
	
	private Map<String, String> sslCertFileTable;
	
	private AtomicLong infoVersion = new AtomicLong(System.currentTimeMillis());
	
	protected volatile ScheduledExecutorService heartbeator = Executors.newSingleThreadScheduledExecutor();

	
	public Tracker(MqServer mqServer, Map<String, String> sslCertFileTable, boolean myServerInTrack, long trackReportIntervalMs){ 
		this.mqServer = mqServer;
		this.myServerAddress = this.mqServer.getServerAddress();
		this.loop = this.mqServer.getEventLoop(); 
		this.myServerInTrack = myServerInTrack; 
		this.sslCertFileTable = sslCertFileTable;
		
		this.heartbeator.scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
				    for(MqClient client : upstreamTrackers.values()){ 
						try{
							ServerEvent event = new ServerEvent();
		    				event.serverInfo = serverInfo();
		    				event.live = true;
		    				
		    				notifyUpstream(client, event);
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						} 
				    }
				} catch (Exception e) {
					log.warn(e.getMessage(), e);
				}
			}
		}, trackReportIntervalMs, trackReportIntervalMs, TimeUnit.MILLISECONDS);
	} 
	
	public ServerInfo serverInfo(){
		return mqServer.serverInfo();
	}
	
	public List<ServerAddress> liveTrackerList(){
		return new ArrayList<ServerAddress>(this.upstreamTrackers.keySet());
	}
	 
	public TrackerInfo trackerInfo(){  
		List<ServerAddress> serverList = new ArrayList<ServerAddress>(this.downstreamTrackers.keySet()); 
		if(myServerInTrack){
			serverList.add(myServerAddress);
			serverTable.put(myServerAddress.toString(), serverInfo());
		}
		TrackerInfo trackerInfo = new TrackerInfo(); 
		trackerInfo.infoVersion = infoVersion.getAndIncrement();
		trackerInfo.serverAddress = myServerAddress; 
		trackerInfo.serverTable = serverTable; 
		
		return trackerInfo;
	}  
	
	public void joinUpstream(List<ServerAddress> trackerList){
		if(trackerList == null || trackerList.isEmpty()) return; 
		
    	for(final ServerAddress trackerAddress : trackerList){  
    		log.info("Connecting to Tracker(%s)", trackerAddress.toString());  
    		final MqClient client = connectToServer(trackerAddress);  
    		client.onDisconnected(new DisconnectedHandler() { 
				@Override
				public void onDisconnected() throws IOException { 
					log.warn("Disconnected from Tracker(%s)", trackerAddress.address);
					healthyUpstreamTrackers.remove(trackerAddress); 
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						return;
					}
    				client.ensureConnectedAsync(); 
    			}  
			});
    		
    		client.onConnected(new ConnectedHandler() {
    			@Override
    			public void onConnected() throws IOException { 
    				log.info("Connected to Tracker(%s)", trackerAddress.address);
    				healthyUpstreamTrackers.put(trackerAddress, client);
    				ServerEvent event = new ServerEvent();
    				event.serverInfo = serverInfo();
    				event.live = true;
    				notifyUpstream(client, event);
    			}
			});
    		upstreamTrackers.put(trackerAddress, client);
    		client.ensureConnectedAsync();
    	}  
	}
	
	private MqClient connectToServer(ServerAddress serverAddress){
		EventLoop driver = loop.duplicate(); //duplicated, no need to close
		if(serverAddress.sslEnabled){
			String certPath = sslCertFileTable.get(serverAddress.address);
			if(certPath != null){
				driver.setClientSslContext(certPath);
			}
		}
		final MqClient client = new MqClient(serverAddress.address, driver);  
		return client;
	}
	
	private void notifyUpstream(MqClient client, ServerEvent event){ 
		Message message = new Message();  
		message.setCommand(Protocol.TRACK_PUB);
		message.setJsonBody(JsonKit.toJSONString(event));  
		message.setAck(false); 
		
		try {  
			client.invokeAsync(message, null);
		} catch (Exception ex) { 
			log.error(ex.getMessage(), ex);
		}    
	} 
	
	
	public void onDownstreamNotified(final ServerEvent event){  
		final ServerAddress serverAddress = event.serverInfo.serverAddress;
		if(myServerAddress.equals(serverAddress)){//myServer changes, just ignore
			return;
		}   
		
		if(event.live){
			serverTable.put(serverAddress.toString(), event.serverInfo);
		}
		
		if(event.live && !downstreamTrackers.containsKey(serverAddress)){ //new downstream tracker
			final MqClient client = connectToServer(serverAddress);  
    		client.onDisconnected(new DisconnectedHandler() { 
				@Override
				public void onDisconnected() throws IOException { 
					log.warn("Server(%s) lost of tracking", serverAddress);
					downstreamTrackers.remove(serverAddress); 
					serverTable.remove(serverAddress.toString());
					publishToSubscribers();   
    			}  
			});
    		
    		client.onConnected(new ConnectedHandler() {
    			@Override
    			public void onConnected() throws IOException { 
    				log.info("Server(%s) in track", serverAddress);
    				downstreamTrackers.put(serverAddress, client);  
					publishToSubscribers();   
    			}
			});
    		try{
    			downstreamTrackers.put(serverAddress, client);
    			client.connectAsync();  //TODO handle failed connections
    		}catch (Exception e) {
				log.error(e.getMessage(), e); 
			} 

		}
		
		if(!event.live){ //server down
			serverTable.remove(serverAddress.toString());
			MqClient downstreamTracker = downstreamTrackers.remove(serverAddress);
			if(downstreamTracker != null){
				try {
					downstreamTracker.close();
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			} 
		}  
		
		publishToSubscribers();  
	}
	
	public void myServerChanged() {
		ServerEvent event = new ServerEvent();
		event.serverInfo = serverInfo();
		event.live = true;
		
		for(MqClient tracker : healthyUpstreamTrackers.values()){
			try{
				notifyUpstream(tracker, event);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}  
		publishToSubscribers();   
	}
	 
	 
	public void subscribe(Message msg, Session session){
		subscribers.add(session);  
		
		Message message = trackerInfoPubMessage();
		try {  
			session.write(message);
		} catch (Exception ex) { 
			log.error(ex.getMessage(), ex);
		}   
	}  
	 
	public void publishToSubscribers(){
		if(subscribers.isEmpty()) return;
		
		Message message = trackerInfoPubMessage();
		for(Session session : subscribers){
			try{
				session.write(message);
			} catch (Exception e) { 
				log.error(e.getMessage(), e);
				subscribers.remove(session);
			}
		}
	} 
	
	private Message trackerInfoPubMessage(){
		Message message = new Message();  
		message.setCommand(Protocol.TRACK_PUB);
		message.setJsonBody(JsonKit.toJSONString(trackerInfo()));
		message.setStatus(200);// server to client
		return message;
	}
	
	public void cleanSubscriberSession(Session session){
		if(subscribers.contains(session)){
			subscribers.remove(session);
		}
	} 
	
	@Override
	public void close() throws IOException {
		this.heartbeator.shutdown();
		for(MqClient client : upstreamTrackers.values()){
			client.close();
		}
		upstreamTrackers.clear();
		for(MqClient client : downstreamTrackers.values()){
			client.close();
		}
		downstreamTrackers.clear(); 
		subscribers.clear(); //No need to close
		
		loop.close(); //duplicated, ok to close
	} 
}
