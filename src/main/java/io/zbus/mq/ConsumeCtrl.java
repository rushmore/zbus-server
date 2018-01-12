package io.zbus.mq;

public class ConsumeCtrl implements Cloneable { 
	private String topic;
	private String consumeGroup;  
	
	private Long offset;    
	
	private Integer consumeWindow; 

	private long consumeTimeout = 10000;
	
	public ConsumeCtrl(){
		
	}  
	
	public ConsumeCtrl(Message msg){ 
		topic = msg.getTopic();
		consumeGroup = msg.getConsumeGroup();  
		offset = msg.getOffset(); 
		consumeWindow = msg.getConsumeWindow();
	}
	
	public void writeToMessage(Message msg){
		msg.setTopic(this.getTopic());
		msg.setConsumeGroup(this.consumeGroup);    
		msg.setOffset(this.offset);
		msg.setConsumeWindow(this.consumeWindow);
	}  
	
	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	} 

	public String getConsumeGroup() {
		return consumeGroup;
	}

	public void setConsumeGroup(String consumeGroup) {
		this.consumeGroup = consumeGroup;
	} 

	public Integer getConsumeWindow() {
		return consumeWindow;
	}

	public void setConsumeWindow(Integer consumeWindow) {
		this.consumeWindow = consumeWindow;
	} 

	public long getConsumeTimeout() {
		return consumeTimeout;
	}

	public void setConsumeTimeout(long consumeTimeout) {
		this.consumeTimeout = consumeTimeout;
	} 

	public Long getOffset() {
		return offset;
	}

	public void setOffset(Long offset) {
		this.offset = offset;
	}

	@Override
	public ConsumeCtrl clone() { 
		try {
			return (ConsumeCtrl)super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}  
}
