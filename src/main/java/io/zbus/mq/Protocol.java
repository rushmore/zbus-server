package io.zbus.mq;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.transport.ServerAddress; 

public class Protocol {  
	public static final String VERSION_VALUE = "0.9.0";      
	
	//=============================[1] Command Values================================================
	//MQ Produce/Consume
	public static final String PRODUCE       = "produce";   
	public static final String CONSUME       = "consume";  
	public static final String ROUTE   	     = "route";     //route back message to sender, designed for RPC 
	public static final String RPC   	     = "rpc";       //the same as produce command except rpc set ack false by default
	
	//Topic control
	public static final String DECLARE = "declare";  
	public static final String QUERY   = "query"; 
	public static final String REMOVE  = "remove";
	public static final String EMPTY   = "empty";   
	
	//High Availability (HA) 
	public static final String TRACK_PUB   = "track_pub"; 
	public static final String TRACK_SUB   = "track_sub";  
	public static final String TRACKER     = "tracker";  
	public static final String SERVER      = "server";  
	
	public static final String TRACE         = "trace";   //trace latest message in server 
	public static final String VERSION       = "version";
	public static final String JS            = "js";      //serve javascript file
	public static final String CSS           = "css";     //serve css file 
	public static final String IMG           = "img";     //serve image file(SVG)
	public static final String PAGE          = "page";    //serve image file(SVG)
	
	public static final String PING          = "ping";    //ping server, returning back server time
	
	
	//=============================[2] Parameter Values================================================
	public static final String COMMAND       		= "cmd";     
	public static final String TOPIC         		= "topic";
	public static final String TOPIC_MASK          	= "topic_mask"; 
	public static final String TAG   	     		= "tag";  
	public static final String OFFSET        		= "offset";
	
	public static final String CONSUME_GROUP        = "consume_group";  
	public static final String GROUP_START_COPY     = "group_start_copy";  
	public static final String GROUP_START_OFFSET   = "group_start_offset";
	public static final String GROUP_START_MSGID    = "group_start_msgid";
	public static final String GROUP_START_TIME     = "group_start_time";   
	public static final String GROUP_FILTER         = "group_filter";  
	public static final String GROUP_MASK           = "group_mask"; 
	public static final String CONSUME_WINDOW       = "consume_window";  
	
	public static final String SENDER   			= "sender"; 
	public static final String RECVER   			= "recver";
	public static final String ID      				= "id";	   
	
	public static final String HOST   			    = "host";  
	public static final String ACK      			= "ack";	  
	public static final String ENCODING 			= "encoding"; 
	
	public static final String ORIGIN_ID     		= "origin_id";
	public static final String ORIGIN_URL   		= "origin_url";
	public static final String ORIGIN_STATUS 		= "origin_status";
	
	//Security 
	public static final String TOKEN   				= "token"; 
	
	
	public static final int MASK_PAUSE    	    = 1<<0; 
	public static final int MASK_RPC    	    = 1<<1; 
	public static final int MASK_EXCLUSIVE 	    = 1<<2;  
	public static final int MASK_DELETE_ON_EXIT = 1<<3; 
	
	public static class ServerEvent{  
		//public ServerAddress serverAddress;
		public ServerInfo serverInfo;
		public boolean live = true;  
	}
	 
	
	public static class TrackItem {
		public ServerAddress serverAddress;  
		public String serverVersion = VERSION_VALUE;  
		public Exception error; //current item error encountered
	}
	
	public static class TrackerInfo extends TrackItem {
		public long infoVersion; 
		public Map<String, ServerInfo> serverTable;
	}
	  
	public static class ServerInfo extends TrackItem {    
		public long infoVersion;
		public List<ServerAddress> trackerList;  
		public Map<String, TopicInfo> topicTable = new ConcurrentHashMap<String, TopicInfo>();  
	}
	
	public static class TopicInfo extends TrackItem { 
		public String topicName;
		public int mask; 
		
		public long messageDepth; //message count on disk
		public int consumerCount; //total consumer count in consumeGroupList
		public List<ConsumeGroupInfo> consumeGroupList = new ArrayList<ConsumeGroupInfo>();
		
		public String creator;
		public long createdTime;
		public long lastUpdatedTime;   
		
		public ConsumeGroupInfo consumeGroup(String name){
			if(consumeGroupList == null) return null;
			for(ConsumeGroupInfo info : consumeGroupList){
				if(name.equals(info.groupName)) return info;
			}
			return null;
		}
	}  
	
	public static class ConsumeGroupInfo{ 
		public String topicName;
		public String groupName;
		public int mask; 
		public String filter;
		public long messageCount;
		public int consumerCount;
		public List<String> consumerList = new ArrayList<String>();
		
		public String creator;
		public long createdTime;
		public long lastUpdatedTime;  
		
		public Exception error; //used only for batch operation indication
	}  
}
