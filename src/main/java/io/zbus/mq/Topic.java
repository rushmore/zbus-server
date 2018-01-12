package io.zbus.mq;

public class Topic implements Cloneable { 
	private String name;
	private Integer mask; 
	
	public Topic(){
		
	}  
	
	public Topic(String name) {
		this.name = name;
	}
	
	public Topic(Message msg){ 
		name = msg.getTopic(); 
		mask = msg.getTopicMask();
	}
	
	public void writeToMessage(Message msg){
		msg.setTopic(this.name);
		msg.setTopicMask(this.mask);
	} 
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}  

	public Integer getMask() {
		return mask;
	}

	public void setMask(Integer topicMask) {
		this.mask = topicMask;
	}

	@Override
	public Topic clone() { 
		try {
			return (Topic)super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}  
}
