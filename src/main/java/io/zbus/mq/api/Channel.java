package io.zbus.mq.api;

public class Channel implements Cloneable {
	private int maxInFlight = 100;
	private final String topic;
	private final String channel;    
	
	public Channel(String topic, String channel){
		this.topic = topic;
		this.channel = channel;
	}
	public Channel(String topic){
		this(topic, null);
	} 
	
	public int getMaxInFlight() {
		return maxInFlight;
	}
	public void setMaxInFlight(int maxInFlight) {
		this.maxInFlight = maxInFlight;
	}  
	
	public String getTopic() {
		return topic;
	}
	public String getChannel() {
		return channel;
	} 
	
	@Override
	public Channel clone() { 
		try {
			return (Channel)super.clone();
		} catch (CloneNotSupportedException e) {
			//ignore
		}
		return null;
	}
	
}