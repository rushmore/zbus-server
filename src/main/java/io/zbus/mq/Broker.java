package io.zbus.mq;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
	
	private Map<ServerAddress, MqClientPool> poolTable = new ConcurrentHashMap<ServerAddress, MqClientPool>();   
	private BrokerRouteTable routeTable = new BrokerRouteTable(); 
	
	private Map<ServerAddress, MqClient> trackerSubscribers = new ConcurrentHashMap<ServerAddress, MqClient>(); 
	
	
	
	private List<ServerNotifyListener> listeners = new ArrayList<ServerNotifyListener>(); 
	private EventLoop eventLoop;  
	private int clientPoolSize = 32; 
	private Map<String, String> sslCertFileTable;
	private String defaultSslCertFile;
	
	private CountDownLatch ready = new CountDownLatch(1);
	private boolean waitCheck = true;
	
	public Broker(){
		this(new BrokerConfig());
	}  
	
	
	public Broker(BrokerConfig config){
		this.eventLoop = new EventLoop(); 
		this.clientPoolSize = config.getClientPoolSize();  
		this.sslCertFileTable = config.getSslCertFileTable();
		this.defaultSslCertFile = config.getDefaultSslCertFile(); 
		
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
	
	public Broker(MqServer server){
		this.eventLoop = new EventLoop();
		this.addTracker(server);
	}
	
	public void addTracker(MqServer server){
		ServerAddress trackerAddress = new ServerAddress();
		trackerAddress.server = server; 
		addTracker(trackerAddress);  
	} 
  
	 
	public void addTracker(String trackerAddress){
		ServerAddress serverAddress = new ServerAddress(trackerAddress); 
		
		addTracker(serverAddress);
	} 
	
	public void addTracker(final ServerAddress trackerAddress){
		if(trackerSubscribers.containsKey(trackerAddress)) return; 
		 
		final AtomicReference<ServerAddress> remoteTrackerAddress = new AtomicReference<ServerAddress>(trackerAddress);
		final MqClient client = connectToServer(trackerAddress); 
		client.onDisconnected(new DisconnectedHandler() { 
			@Override
			public void onDisconnected() throws IOException { 
				ServerAddress trackerAddress = remoteTrackerAddress.get();
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
				ServerAddress trackerAddress = remoteTrackerAddress.get();
				log.info("Connected to tracker(%s)", trackerAddress);  
				
				Message req = new Message();  
				req.setCommand(Protocol.TRACK_SUB);
				req.setAck(false); 
				client.invokeAsync(req, null);
			}
		}); 
		
		client.onMessage(new MessageHandler<Message>() {  
			@Override
			public void handle(Message msg, Session session) throws IOException { 
				if(Protocol.TRACK_PUB.equals(msg.getCommand())){  
					TrackerInfo trackerInfo = JsonKit.parseObject(msg.getBodyString(), TrackerInfo.class);
					
					remoteTrackerAddress.set(trackerInfo.serverAddress);
					//remote tracker's real address obtained, update ssl cert file mapping
					if(trackerInfo.serverAddress.sslEnabled){
						String sslCertFile = sslCertFileTable.get(trackerAddress.address);
						if(sslCertFile != null){
							sslCertFileTable.put(trackerInfo.serverAddress.address, sslCertFile);
						}
					} 
					
					if(trackerAddress.server != null){ //InProc 
						String tcpKey = trackerInfo.serverAddress.toString();
						if(trackerInfo.serverTable.containsKey(tcpKey)){
							ServerInfo serverInfo = trackerInfo.serverTable.remove(tcpKey);
							serverInfo.serverAddress = trackerAddress;
							trackerInfo.serverTable.put(trackerAddress.toString(), serverInfo);
							for(TopicInfo topicInfo : serverInfo.topicTable.values()){
								topicInfo.serverAddress = trackerAddress;
							}
						}
						trackerInfo.serverAddress = trackerAddress;
					}
					
					List<ServerAddress> toRemove = routeTable.updateTracker(trackerInfo);
					for(ServerInfo serverInfo : routeTable.serverTable().values()){
						addServer(serverInfo);
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
			}
		});
		
		client.ensureConnectedAsync();
		trackerSubscribers.put(trackerAddress, client); 
	} 
	
	private void addServer(final ServerAddress serverAddress) throws IOException { 
		MqClientPool pool = null;  
		synchronized (poolTable) {
			pool = poolTable.get(serverAddress);
			if(pool != null) return; 
			 
			try{
				pool = createMqClientPool(serverAddress, serverAddress);
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
	
	private void addServer(final ServerInfo serverInfo) throws IOException {  
		addServer(serverInfo.serverAddress);
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
	
	
	public void addSslCertFile(String address, String certPath){
		sslCertFileTable.put(address, certPath);
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
				ready.await(3000, TimeUnit.MILLISECONDS); 
			} catch (InterruptedException e) {
				//ignore 
			}
			waitCheck = false;
		}
	}

	private MqClient connectToServer(ServerAddress serverAddress){
		EventLoop loop = eventLoop.duplicate(); //duplicated, no need to close
		if(serverAddress.sslEnabled){
			String certPath = sslCertFileTable.get(serverAddress.address);
			if(certPath == null) certPath = defaultSslCertFile;
			if(certPath == null){
				throw new IllegalStateException(serverAddress + " certificate file not found");
			}
			loop.setClientSslContext(certPath); 
		}
		
		return new MqClient(serverAddress, loop);   
	}
	
	private MqClientPool createMqClientPool(ServerAddress remoteServerAddress, ServerAddress serverAddress){
		EventLoop loop = eventLoop.duplicate(); //duplicated, no need to close
		if(serverAddress.sslEnabled){
			String certPath = sslCertFileTable.get(remoteServerAddress.address);
			if(certPath == null) certPath = sslCertFileTable.get(serverAddress.address);
			if(certPath == null) certPath = defaultSslCertFile;
			
			if(certPath == null){
				throw new IllegalStateException(serverAddress + " certificate file not found");
			}
			loop.setClientSslContext(certPath);
		}
		return new MqClientPool(serverAddress, clientPoolSize, loop);   
	} 
	
	public void setDefaultSslCertFile(String defaultSslCertFile) {
		this.defaultSslCertFile = defaultSslCertFile;
	} 
	
	public static interface ServerSelector { 
		ServerAddress[] select(BrokerRouteTable table, Message message); 
	} 
	
	public static interface ServerNotifyListener { 
		void onServerJoin(MqClientPool server); 
		void onServerLeave(ServerAddress serverAddress);
	}	
}
