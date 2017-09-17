package io.zbus.mq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.mq.Protocol.TrackerInfo;
import io.zbus.transport.ServerAddress;
 
public class BrokerRouteTable {    
	private long lastUpdatedTime = System.currentTimeMillis();
	private double voteFactor = 0.5;
	//{ TrackerAddress=>[ServerAddress] }
	private Map<ServerAddress, Vote> votesTable = new ConcurrentHashMap<ServerAddress, Vote>(); 
	//{ ServerAddress=>ServerInfo }
	private Map<ServerAddress, ServerInfo> serverTable = new HashMap<ServerAddress, ServerInfo>();
	//{ TopicName=> { ServerAddress=>TopicInfo } }
	private Map<String, Map<ServerAddress, TopicInfo>> topicTable = new HashMap<String, Map<ServerAddress, TopicInfo>>(); 
	
	
	private static class Vote {
		long version; 
		Set<ServerAddress> trackedServers;
	}
	
	public List<ServerAddress> updateTracker(TrackerInfo trackerInfo){  
		lastUpdatedTime = System.currentTimeMillis();
		//1) Update Votes
		Vote vote = votesTable.get(trackerInfo.serverAddress);
		if(vote != null && vote.version >= trackerInfo.infoVersion){
			return new ArrayList<ServerAddress>(); //empty to remove
		}
		
		HashSet<ServerAddress> trackedServers = new HashSet<ServerAddress>();
		for(ServerInfo serverInfo : trackerInfo.serverTable.values()){
			trackedServers.add(serverInfo.serverAddress);
		}
		if(vote == null) {
			vote = new Vote();
			vote.version = trackerInfo.infoVersion;  
		}  
		vote.trackedServers = trackedServers;  
		votesTable.put(trackerInfo.serverAddress, vote);
		
		//Update serverTable
		for(Entry<String, ServerInfo> e : trackerInfo.serverTable.entrySet()){
			ServerInfo serverInfo = e.getValue();
			ServerInfo oldSeverInfo = this.serverTable.get(serverInfo.serverAddress);
			if(oldSeverInfo != null && oldSeverInfo.infoVersion >= serverInfo.infoVersion) continue; 
			
			this.serverTable.put(serverInfo.serverAddress, serverInfo);
		}
		
		return this.purge(); 
	}
	
	public List<ServerAddress> removeTracker(ServerAddress trackerAddress){
		lastUpdatedTime = System.currentTimeMillis();
		
		if(!votesTable.containsKey(trackerAddress)) {
			return new ArrayList<ServerAddress>();
		}
		this.votesTable.remove(trackerAddress);
		
		return this.purge();
	}
	
	
	private List<ServerAddress> purge(){
		List<ServerAddress> toRemove = new ArrayList<ServerAddress>();
		
		Map<ServerAddress, ServerInfo> serverTableLocal = new HashMap<ServerAddress, ServerInfo>(this.serverTable);
		for(ServerAddress serverAddress : serverTableLocal.keySet()) { 
			int count = 0;
			for(Vote vote : this.votesTable.values()){
				if(vote.trackedServers.contains(serverAddress)) count++;
			}
			if(count < votesTable.size()*voteFactor){
				toRemove.add(serverAddress);
			} 
		}
		for(ServerAddress serverAddress : toRemove){
			serverTableLocal.remove(serverAddress);
		}
		
		this.serverTable = serverTableLocal;
		rebuildTopicTable(serverTableLocal); 
		
		return toRemove;
	}
	
	private void rebuildTopicTable(Map<ServerAddress, ServerInfo> serverTableLocal) { 
		Map<String, Map<ServerAddress, TopicInfo>> table = new ConcurrentHashMap<String, Map<ServerAddress, TopicInfo>>();
		for (ServerInfo serverInfo : serverTableLocal.values()) {
			for (TopicInfo topicInfo : serverInfo.topicTable.values()) {
				Map<ServerAddress, TopicInfo> server2Topic = table.get(topicInfo.topicName);
				if (server2Topic == null) {
					server2Topic = new HashMap<ServerAddress, TopicInfo>();
					table.put(topicInfo.topicName, server2Topic);
				}
				server2Topic.put(topicInfo.serverAddress, topicInfo);
			}
		}
		this.topicTable = table;
	} 
	
	public Map<ServerAddress, ServerInfo> serverTable() {
		return serverTable;
	} 
 
	public Map<String, Map<ServerAddress, TopicInfo>> topicTable(){
		return this.topicTable;
	} 
	
	public long getLastUpdatedTime() {
		return lastUpdatedTime;
	}
}