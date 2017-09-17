package io.zbus.mq.server;
 
import io.zbus.mq.Message;
import io.zbus.mq.Protocol;

/**
 * Fixiture of older version of zbus
 * 
 * @author Rushmore
 *
 */
public class Fix {
	public static boolean Enabled = Boolean.valueOf(System.getProperty("zbus7", "false"));
	
	public static final String QueryMQ   = "query_mq";  //NEW: query
	public static final String CreateMQ  = "create_mq"; //NEW: declare
	public static final String RemoveMQ  = "remove_mq"; //NEW: remove
	public static final String MQ  		 = "mq";        //NEW: topic
	public static final String MqName    = "mq_name"; 
	public static final String MqMode    = "mq_mode"; 
	
	public static final String OriginId    = "rawid";      //original id
	public static final String OriginUrl   = "origin_url"; //original URL  
	public static final String OriginStatus= "reply_code"; //original Status 
	
	
	public static final String Topic   = "topic";  //May be zbus7's topic or NEW topic, conflicts! 
	public static final int RPC = 1<<3; 
	
	public static String getTopic(Message msg){ 
		if(!Enabled) return null;
		
		String topic = msg.getHeader(Topic); 
		String mq = msg.getHeader(MQ); //OLD
				
		if(topic != null && mq != null) return mq; //ignore zbus7's topic
		if(topic != null) return topic;
		if(mq != null) return mq; 
		
		topic = msg.getHeader(MqName);
		if(topic != null) return topic;
		
		return null;
	}
	
	public static Integer getTopicMask(Message msg){ 
		if(!Enabled) return null;
		
		String value = msg.getHeader(MqMode);
		if(value != null && (Integer.valueOf(value)&RPC) != 0 ){
			return Protocol.MASK_MEMORY; //RPC => memory message queue
		} 
		
		return null;
	}
	
	public static String getOriginId(Message msg){ 
		if(!Enabled) return null;
		
		return msg.getHeader(Fix.OriginId); 
	}
	
	public static void setOriginId(Message msg, String value){ 
		if(!Enabled) return;
		
		msg.setHeader(Fix.OriginId, value);  
	}
	
	public static String getOriginUrl(Message msg){  
		if(!Enabled) return null;
		
		return msg.getHeader(Fix.OriginUrl); 
	}
	
	public static void setOriginUrl(Message msg, String value){ 
		if(!Enabled) return;
		
		msg.setHeader(Fix.OriginUrl, value); 
	}
	
	public static Integer getOriginStatus(Message msg){   
		if(!Enabled) return null;
		
		String value = msg.getHeader(Fix.OriginStatus); 
		if(value != null) Integer.valueOf(value); 
		
		return null;
	}
	
	public static void setOriginStatus(Message msg, Integer value){  
		if(!Enabled) return;
		
		msg.setHeader(Fix.OriginStatus, value); 
	}
}
