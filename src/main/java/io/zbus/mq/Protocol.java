package io.zbus.mq;

import java.util.List;

public interface Protocol {  
	//Parameter keys(Message main key-value pairs)
	public static final String CMD       = "cmd";       // Request message command
	public static final String STATUS    = "status";    // Response message status
	public static final String ID        = "id";        // Message ID
	public static final String BODY      = "body";      // Message body 
	public static final String API_KEY   = "apiKey";    // Authentication public Key
	public static final String SIGNATURE = "signature"; // Authentication signature generated
	
	//Command values(key=cmd)
	public static final String PUB    = "pub";      //Publish message
	public static final String SUB    = "sub";      //Subscribe message stream
	public static final String TAKE   = "take";     //One-time read message from MQ 
	public static final String ROUTE  = "route";    //Route message to specified sender client
	public static final String CREATE = "create";   //Create or Update
	public static final String REMOVE = "remove";   //Remove MQ/Channel
	public static final String QUERY  = "query";    //Query MQ/Channel
	public static final String BIND   = "bind";     //Bind URL mapping to MQ message(Make browser friendly)
	public static final String PING   = "ping";     //Heartbeat ping
	
	//Parameter keys(for commands)
	public static final String MQ             = "mq";  
	public static final String CHANNEL        = "channel";  
	public static final String FILTER         = "filter";    //Filter on message's tag
	public static final String TAG            = "tag";       //Tag of message, if filter applied
	public static final String OFFSET         = "offset";
	public static final String CHECKSUM       = "checksum";  //Offset checksum
	public static final String SOURCE         = "source";    //message's source id(socket)
	public static final String TARGET         = "target";    //route message's target id(socket)
	public static final String MQ_TYPE        = "mqType";  
	public static final String MQ_MASK        = "mqMask";  
	public static final String CHANNEL_MASK   = "channelMask"; 
	public static final String WINDOW         = "window";
	public static final String ACK            = "ack";  
	public static final String CLEAR_BIND     = "clearBind";  
	
	//Parameter mqType values
	public static final String MEMORY  = "memory";  
	public static final String DISK    = "disk";  
	public static final String DB      = "db";  
	
	public static class MqInfo {   
		public String name; 
		public Integer mask; 
		public long size;
		public String type;
		public List<ChannelInfo> channels; 
	}

	public static class ChannelInfo {   
		public String name; 
		public Integer mask;  
		public String filter;
		public long offset;
	}
	
}
