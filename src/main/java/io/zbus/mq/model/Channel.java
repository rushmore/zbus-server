package io.zbus.mq.model;

/**
 * 
 * Channel represents an isolated MQ's data reader.
 * It controls offset from MQ's start position.
 * 
 * @author leiming.hong
 *
 */
public class Channel implements Cloneable {   
	public final String name;
	public Long offset;  
	public Integer mask; 
	public String filter; 
	public String offsetChecksum; //MsgId
	
	public Channel(String name, Long offset) {
		this.name = name;
		this.offset = offset;
	}
	public Channel(String name) {
		this(name, null);
	}
	
	@Override
	public Channel clone() { 
		try {
			Channel channel = (Channel)super.clone(); 
			return channel;
		} catch (CloneNotSupportedException e) { 
			throw new IllegalStateException(e.getMessage(), e.getCause());
		}   
	}
}
