package io.zbus.mq.api;

public class ConsumeGroup implements Cloneable {
	private int maxInFlight = 100;
	private final String topic;
	private final String consumeGroup;    
	
	public ConsumeGroup(String topic, String consumeGroup){
		this.topic = topic;
		this.consumeGroup = consumeGroup;
	}
	public ConsumeGroup(String topic){
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
	public String getConsumeGroup() {
		return consumeGroup;
	} 
	
	@Override
	public ConsumeGroup clone() { 
		try {
			return (ConsumeGroup)super.clone();
		} catch (CloneNotSupportedException e) {
			//ignore
		}
		return null;
	}
	
}