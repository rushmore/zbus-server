package io.zbus.mq;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.zbus.kit.JsonKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.mq.Protocol.TrackerInfo;
import io.zbus.mq.server.MqServer;
import io.zbus.transport.Client.ConnectedHandler;
import io.zbus.transport.Client.DisconnectedHandler;
import io.zbus.transport.EventLoop;
import io.zbus.transport.MessageHandler;
import io.zbus.transport.ServerAddress;
import io.zbus.transport.Session; 

public class Broker implements Closeable { 
	private static final Logger log = LoggerFactory.getLogger(Broker.class); 
	private BrokerRouteTable routeTable = new BrokerRouteTable();  
	
	private Map<ServerAddress, MqClientPool> poolTable = new ConcurrentHashMap<ServerAddress, MqClientPool>();    
	private Map<ServerAddress, MqClient> trackerSubscribers = new ConcurrentHashMap<ServerAddress, MqClient>(); 
	 
	private List<ServerNotifyListener> listeners = new ArrayList<ServerNotifyListener>(); 
	
	private EventLoop eventLoop;
	
	private int clientPoolSize = 32;   
	private int readyTimeout = 3000; //3 seconds
	
	private CountDownLatch ready = new CountDownLatch(1);
	private boolean waitCheck = true;
	
	
	public Broker(){
		this(new BrokerConfig());
	}   
	
	public Broker(BrokerConfig config){
		this.eventLoop = new EventLoop(); 
		this.clientPoolSize = config.getClientPoolSize();
		
		List<ServerAddress> trackerList = config.getTrackerList(); 
		for(ServerAddress serverAddress : trackerList){ 
			addTracker(serverAddress); 
		}   
	}   
	
	public Broker(String trackerList){
		this.eventLoop = new EventLoop();  
		
		String[] bb = trackerList.split("[,; ]");
		for(String tracker : bb){
			tracker = tracker.trim();
			if(tracker.isEmpty()) continue; 
			addTracker(tracker);
		}
	} 
	
	public Broker(ServerAddress trackerAddress){
		this.eventLoop = new EventLoop();  
		addTracker(trackerAddress); 
	} 
	
	public Broker(MqServer server){
		this(server, null);
	} 
	
	public Broker(MqServer server, String token){
		this.eventLoop = new EventLoop();
		this.addTracker(server, token);
	} 
	
	
	public void addTracker(MqServer server){
		addTracker(server, null);
	}  
	
	public void addTracker(MqServer server, String token){
		ServerAddress trackerAddress = new ServerAddress();
		trackerAddress.server = server;  
		trackerAddress.token = token;
		addTracker(trackerAddress);  
	}  
	 
	public void addTracker(String trackerAddress){
		ServerAddress serverAddress = new ServerAddress(trackerAddress);  
		addTracker(serverAddress);
	} 
	
	
	public void addTracker(ServerAddress serverAddress){
		final ServerAddress trackerAddress = serverAddress.clone();
		
		if(trackerSubscribers.containsKey(trackerAddress)) return;   
		
		final MqClient client = new MqClient(trackerAddress, this.eventLoop);
		trackerSubscribers.put(trackerAddress, client); 
		
		client.onDisconnected(new DisconnectedHandler() { 
			@Override
			public void onDisconnected() throws IOException {  
				log.warn("Disconnected from tracker(%s)", trackerAddress);  
				List<ServerAddress> toRemove = routeTable.removeTracker(trackerAddress);  
				if(!toRemove.isEmpty()){ 
					for(ServerAddress serverAddress : toRemove){
						removeServer(serverAddress);
					}
				}  
				
				client.ensureConnectedAsync(); 
			}  
		});
		
		client.onConnected(new ConnectedHandler() {
			@Override
			public void onConnected() throws IOException {  
				log.info("Connected to tracker(%s)", trackerAddress);  
				
				Message req = new Message();  
				req.setCommand(Protocol.TRACK_SUB); 
				req.setToken(trackerAddress.getToken());
				client.invokeAsync(req, null);
			}
		}); 
		
		client.onMessage(new MessageHandler<Message>() {  
			@Override
			public void handle(Message msg, Session session) throws IOException { 
				if(msg.getStatus() != 200){
					log.error(msg.getBodyString());
					return;
				}
				if(!Protocol.TRACK_PUB.equals(msg.getCommand())){ 
					log.error("Unknown message: " + msg);
				}
				 
				TrackerInfo trackerInfo = JsonKit.parseObject(msg.getBodyString(), TrackerInfo.class); 
				
				trackerAddress.setAddress(trackerInfo.serverAddress.address); //!!update remote address!!
				
				if(trackerAddress.getServer() != null){ //InProc, change TrackerInfo
					String tcpKey = trackerInfo.serverAddress.toString();
					trackerInfo.serverAddress = trackerAddress;
					if(trackerInfo.serverTable.containsKey(tcpKey)){
						ServerInfo serverInfo = trackerInfo.serverTable.remove(tcpKey);
						serverInfo.serverAddress = trackerAddress;
						trackerInfo.serverTable.put(trackerAddress.toString(), serverInfo);
						for(TopicInfo topicInfo : serverInfo.topicTable.values()){
							topicInfo.serverAddress = trackerAddress;
						}
					} 
				}
				
				List<ServerAddress> toRemove = routeTable.updateTracker(trackerInfo);
				for(ServerInfo serverInfo : routeTable.serverTable().values()){
					addServer(serverInfo, trackerAddress);
				}
				
				if(!toRemove.isEmpty()){ 
					for(ServerAddress serverAddress : toRemove){
						removeServer(serverAddress);
					}
				}  
				
				if(waitCheck){
					ready.countDown();
				}
			} 
		});
		
		client.ensureConnectedAsync(); 
	} 
	
	private void addServer(final ServerAddress serverAddress, ServerAddress trackerAddress) throws IOException { 
		MqClientPool pool = null;  
		synchronized (poolTable) {
			pool = poolTable.get(serverAddress);
			if(pool != null) return; 
			 
			try {
				if(serverAddress.isSslEnabled()){ 
					MqClient client = new MqClient(trackerAddress, this.eventLoop); 
					String certificate = client.querySslCertificate(serverAddress.getAddress());
					client.close();
					if(certificate == null){
						throw new IllegalStateException("MqServer SSL enabled, but certificate not found");
					}
					serverAddress.setCertificate(certificate);
				}
				pool = new MqClientPool(serverAddress, clientPoolSize, this.eventLoop);   
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return;
			} 
			poolTable.put(serverAddress, pool);  
		}   
		
		final MqClientPool createdPool = pool;
		eventLoop.getGroup().submit(new Runnable() { 
			@Override
			public void run() {  
				try { 
					for(final ServerNotifyListener listener : listeners){
						eventLoop.getGroup().submit(new Runnable() { 
							@Override
							public void run() {  
								listener.onServerJoin(createdPool);
							}
						});
					} 
				} catch (Exception e) { 
					log.error(e.getMessage(), e);
				}  
			}
		}); 
	}
	
	private void addServer(final ServerInfo serverInfo, ServerAddress trackerAddress) throws IOException {  
		addServer(serverInfo.serverAddress, trackerAddress);
	}  
	 
	private void removeServer(final ServerAddress serverAddress) { 
		final MqClientPool pool;
		synchronized (poolTable) { 
			pool = poolTable.remove(serverAddress);
			if(pool == null) return;   
		}    
		
		eventLoop.getGroup().schedule(new Runnable() { 
			@Override
			public void run() {
				try {
					pool.close();
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				} 
			}
		}, 1000, TimeUnit.MILLISECONDS); //delay 1s to close to wait other service depended on this broker
		
		for(final ServerNotifyListener listener : listeners){
			eventLoop.getGroup().submit(new Runnable() { 
				@Override
				public void run() { 
					listener.onServerLeave(serverAddress);
				}
			});
		}
	}  
	
	public EventLoop getEventLoop() {
		return eventLoop;
	}
	
	@Override
	public void close() throws IOException {
		for(MqClient client : trackerSubscribers.values()){
			client.close();
		}
		trackerSubscribers.clear();
		synchronized (poolTable) {
			for(MqClientPool pool : poolTable.values()){ 
				pool.close();
			}
			poolTable.clear();
		}  
		
		eventLoop.close();
	}   
	
	public MqClientPool[] selectClient(ServerSelector selector, Message msg) {
		checkReady();
		
		ServerAddress[] serverList = selector.select(routeTable, msg);
		if(serverList == null){
			return poolTable.values().toArray(new MqClientPool[0]);
		}
		
		MqClientPool[] pools = new MqClientPool[serverList.length];
		int count = 0;
		for(int i=0; i<serverList.length; i++){
			pools[i] = poolTable.get(serverList[i]);
			if(pools[i] != null) count++;
		} 
		if(count == serverList.length) return pools; 

		MqClientPool[] pools2 = new MqClientPool[count];
		int j = 0;
		for(int i=0; i<pools.length; i++){
			if(pools[i] != null){
				pools2[j++] = pools[i];
			}
		} 
		return pools2;
	} 
	 
	public void addServerNotifyListener(ServerNotifyListener listener) {
		this.listeners.add(listener);
	}
 
	public void removeServerNotifyListener(ServerNotifyListener listener) {
		this.listeners.remove(listener);
	} 
	 
	 
	private void checkReady(){
		if(waitCheck){
			try {
				ready.await(readyTimeout, TimeUnit.MILLISECONDS); 
			} catch (InterruptedException e) {
				//ignore 
			}
			waitCheck = false;
		}
	}
  
	public static interface ServerSelector { 
		ServerAddress[] select(BrokerRouteTable table, Message message); 
	} 
	
	public static interface ServerNotifyListener { 
		void onServerJoin(MqClientPool server); 
		void onServerLeave(ServerAddress serverAddress);
	}	
}
