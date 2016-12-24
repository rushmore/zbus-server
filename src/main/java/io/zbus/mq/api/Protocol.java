package io.zbus.mq.api;

public interface Protocol { 
	public static final String PRODUCE 			= "produce";
	public static final String CONSUME 			= "consume";
	
	public static final String PRODUCE_ACK      = "produce-ack";
	public static final String CONSUME_ACK 	    = "consume-ack";
	
	public static final String STREAM 			= "stream";
	
	public static final String QUIT 		    = "quit";
	
	public static final String RESPONSE			= "response";
	
	public static final String DECLARE_TOPIC 	= "declare-topic"; 
	public static final String QUERY_TOPIC	    = "query-topic"; 
	public static final String REMOVE_TOPIC	    = "remove-topic";
	
	public static final String DECLARE_CHANNEL 	= "declare-channel";
	public static final String QUERY_CHANNEL	= "query-channel";
	public static final String REMOVE_CHANEEL	= "remove-channel";
}
