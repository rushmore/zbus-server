package io.zbus.mq.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import io.zbus.mq.server.auth.AuthProvider;
import io.zbus.mq.server.auth.Token;
import io.zbus.transport.Client.ConnectedHandler;
import io.zbus.transport.Client.DisconnectedHandler;
import io.zbus.transport.ServerAddress;
import io.zbus.transport.Session; 
 

/**
 * Act as both TrackServer and TrackClient
 * 
 * @author rushmore
 *
 */
public class Tracker implements Closeable{
	private static final Logger log = LoggerFactory.getLogger(Tracker.class);  
	
	private AtomicLong infoVersion = new AtomicLong(System.currentTimeMillis());
	
	private Map<ServerAddress, MqClient> serversInTrack = new ConcurrentHashMap<ServerAddress, MqClient>();  
	private Map<ServerAddress, MqClient> healthyTrackers = new ConcurrentHashMap<ServerAddress, MqClient>();
	private Map<ServerAddress, MqClient> trackers = new ConcurrentHashMap<ServerAddress, MqClient>();  
	private Set<Session> subscribedClients = new HashSet<Session>();
	
	private Map<String, ServerInfo> serverInfoTable = new ConcurrentHashMap<String, ServerInfo>();  
	
	private MqServer mqServer;  
	private AuthProvider authProvider;
	private ScheduledExecutorService reportToTracker = Executors.newSingleThreadScheduledExecutor();

	
	public Tracker(final MqServer mqServer){ 
		this.mqServer = mqServer;   
		this.authProvider = mqServer.getConfig().getAuthProvider();
		
		long reportToTrackerInterval = mqServer.getConfig().getReportToTrackerInterval();
		this.reportToTracker.scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
				    for(MqClient client : trackers.values()){ 
						try{
							ServerEvent event = new ServerEvent();
							event.certificate = mqServer.getServerAddress().getCertificate();
		    				event.serverInfo = mqServer.serverInfo();
		    				event.live = true;
		    				
		    				publishToTracker(client, event);
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						} 
				    }
				} catch (Exception e) {
					log.warn(e.getMessage(), e);
				}
			}
		}, reportToTrackerInterval, reportToTrackerInterval, TimeUnit.MILLISECONDS);
	} 
	
	public ServerInfo serverInfo(Token token){
		ServerInfo info = mqServer.serverInfo(); 
		return Token.filter(info, token);
	}   
	 
	public TrackerInfo trackerInfo(Token token){  
		List<ServerAddress> serverList = new ArrayList<ServerAddress>(this.serversInTrack.keySet()); 
		ServerAddress trackerAddress = mqServer.getServerAddress();
		if(!mqServer.getConfig().isTrackerOnly()){
			serverList.add(mqServer.getServerAddress());
			serverInfoTable.put(trackerAddress.toString(), mqServer.serverInfo());
		}
		
		TrackerInfo trackerInfo = new TrackerInfo(); 
		trackerInfo.infoVersion = infoVersion.getAndIncrement();
		trackerInfo.serverAddress = trackerAddress;   
		trackerInfo.serverTable = new HashMap<String, ServerInfo>(); 
		for(Entry<String, ServerInfo> e : serverInfoTable.entrySet()){
			ServerInfo serverInfo = e.getValue();
			trackerInfo.serverTable.put(e.getKey(), Token.filter(serverInfo, token));
		}  
		
		return trackerInfo;
	}  
	 
	
	public List<ServerAddress> trackerList(){
		return new ArrayList<ServerAddress>(this.trackers.keySet());
	}
	
	public void joinTracker(List<ServerAddress> trackerList){
		if(trackerList == null || trackerList.isEmpty()) return; 
		
    	for(final ServerAddress trackerAddress : trackerList){  
    		log.info("Connecting to Tracker(%s)", trackerAddress.toString());  
    		final MqClient client = new MqClient(trackerAddress, mqServer.getEventLoop());  
    		client.attr("tracker", trackerAddress);
    		
    		client.onDisconnected(new DisconnectedHandler() { 
				@Override
				public void onDisconnected() throws IOException { 
					log.warn("Disconnected from Tracker(%s)", trackerAddress.address);
					healthyTrackers.remove(trackerAddress); 
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
    				healthyTrackers.put(trackerAddress, client);
    				ServerEvent event = new ServerEvent();
    				event.serverInfo = mqServer.serverInfo();
    				event.certificate = mqServer.getServerAddress().getCertificate();
    				event.live = true;
    				
    				publishToTracker(client, event);
    			}
			});
    		trackers.put(trackerAddress, client);
    		client.ensureConnectedAsync();
    	}  
	}  
	
	public void serverInTrackUpdated(final ServerEvent event){  
		final ServerAddress serverAddress = event.serverInfo.serverAddress.clone(); 
		if(event.certificate != null){ //update certifcate of tracked server if SSL enabled
			serverAddress.setCertificate(event.certificate);
			mqServer.sslCertTable.put(serverAddress.getAddress(), event.certificate);
		}
		
		if(mqServer.getServerAddress().equals(serverAddress)){//myServer changes, just ignore
			return;
		}   
		
		if(event.live){
			serverInfoTable.put(serverAddress.toString(), event.serverInfo);
		}
		
		if(event.live && !serversInTrack.containsKey(serverAddress)){ //new downstream tracker
			final MqClient client = new MqClient(serverAddress, mqServer.getEventLoop());  
    		client.onDisconnected(new DisconnectedHandler() { 
				@Override
				public void onDisconnected() throws IOException { 
					log.warn("Server(%s) lost of tracking", serverAddress);
					serversInTrack.remove(serverAddress); 
					serverInfoTable.remove(serverAddress.toString());
					publishToClient();   
    			}  
			});
    		
    		client.onConnected(new ConnectedHandler() {
    			@Override
    			public void onConnected() throws IOException { 
    				log.info("Server(%s) in track", serverAddress);
    				serversInTrack.put(serverAddress, client);  
					publishToClient();   
    			}
			});
    		try{
    			serversInTrack.put(serverAddress, client);
    			client.connectAsync();  //TODO handle failed connections
    		}catch (Exception e) {
				log.error(e.getMessage(), e); 
			} 

		}
		
		if(!event.live){ //server down
			serverInfoTable.remove(serverAddress.toString());
			MqClient downstreamTracker = serversInTrack.remove(serverAddress);
			if(downstreamTracker != null){
				try {
					downstreamTracker.close();
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			} 
		}  
		
		publishToClient();  
	}
	
	private void publishToTracker(MqClient client, ServerEvent event){  
		Message message = new Message();  
		message.setCommand(Protocol.TRACK_PUB);
		message.setJsonBody(JsonKit.toJSONString(event));  
		message.setAck(false); 
		
		ServerAddress trackerAddress = client.attr("tracker");
		if(trackerAddress != null){
			message.setToken(trackerAddress.getToken());
		}
		
		try {  
			client.invokeAsync(message, null);
		} catch (Exception ex) { 
			log.error(ex.getMessage(), ex);
		}    
	} 
	
	public void myServerChanged() {
		ServerEvent event = new ServerEvent();
		event.serverInfo = mqServer.serverInfo();
		event.certificate = mqServer.getServerAddress().getCertificate();
		event.live = true;
		
		for(MqClient tracker : healthyTrackers.values()){
			try{
				publishToTracker(tracker, event);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}  
		publishToClient();   
	}
	 
	 
	public void clientSubcribe(Message msg, Session session){
		subscribedClients.add(session); 
		Token token = authProvider.getToken(msg.getToken()); 
		session.attr("token", token);
		
		Message message = trackerInfoPubMessage(token);
		try {  
			session.write(message);
		} catch (Exception ex) { 
			log.error(ex.getMessage(), ex);
		}   
	}  
	 
	public void publishToClient(){
		if(subscribedClients.isEmpty()) return; 
		
		for(Session session : subscribedClients){
			try{
				Token token = session.attr("token");
				Message message = trackerInfoPubMessage(token); 
				session.write(message);
			} catch (Exception e) { 
				log.error(e.getMessage(), e);
				subscribedClients.remove(session);
			}
		}
	} 
	
	private Message trackerInfoPubMessage(Token token){
		Message message = new Message();  
		message.setCommand(Protocol.TRACK_PUB);
		message.setJsonBody(JsonKit.toJSONString(trackerInfo(token))); 
		message.setStatus(200);// server to client
		return message;
	}
	
	public void cleanSession(Session session){
		if(subscribedClients.contains(session)){
			subscribedClients.remove(session);
		}
	} 
	
	@Override
	public void close() throws IOException {
		this.reportToTracker.shutdown();
		for(MqClient client : trackers.values()){
			client.close();
		}
		trackers.clear();
		for(MqClient client : serversInTrack.values()){
			client.close();
		}
		serversInTrack.clear(); 
		subscribedClients.clear(); //No need to close 
	} 
}
