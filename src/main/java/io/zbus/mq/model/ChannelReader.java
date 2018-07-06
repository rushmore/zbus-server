package io.zbus.mq.model;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import io.zbus.mq.Protocol.ChannelInfo;
import io.zbus.transport.Message;

public interface ChannelReader extends Closeable  {  
	
	Message read() throws IOException;  
	
	List<Message> read(int count) throws IOException;  
	
	boolean seek(Long offset, String msgid) throws IOException; 
	
	void destroy();
	
	boolean isEnd(); 

	void setFilter(String filter);   
	
	String getFilter();
	
	Integer getMask(); 
	
	void setMask(Integer mask);
	
	ChannelInfo info(); 
}