package io.zbus.mq.memory;

import java.io.IOException;
import java.util.List;

import io.zbus.mq.Protocol;
import io.zbus.mq.model.Channel;
import io.zbus.mq.model.ChannelReader;
import io.zbus.mq.model.MessageQueue.AbstractMessageQueue;
import io.zbus.transport.Message;

public class MemoryQueue extends AbstractMessageQueue{  
	private CircularArray data;   
	private Integer mask = 0; 
	
	public MemoryQueue(String name, int maxSize) { 
		super(name); 
		this.data = new CircularArray(maxSize); 
	}  
	
	public MemoryQueue(String name) { 
		this(name, 1000); 
	} 
	
	@Override
	public String type() { 
		return Protocol.MEMORY;
	}
	
	@Override
	public long size() { 
		return data.size();
	}
	
	@Override
	protected ChannelReader buildChannelReader(String channelId) throws IOException {
		Channel channel = new Channel(channelId);
		return new MemoryChannelReader(data, channel);
	}
	 
	@Override
	public void write(Message message) {  
		data.write(message);
	} 
	
	@Override
	public void write(List<Message> messages) { 
		data.write(messages.toArray());
	} 
   
	@Override
	public Integer getMask() { 
		return mask;
	}
	
	@Override
	public void setMask(Integer mask) {
		this.mask = mask;
	}
	
	@Override
	public void flush() {  
		
	}
	
	@Override
	public void destroy() { 
		
	}
}
