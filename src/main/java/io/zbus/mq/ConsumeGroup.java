package io.zbus.mq;

import java.util.concurrent.TimeUnit;

public class ConsumeGroup implements Cloneable { 
	private String groupName;
	private Boolean groupNameAuto;
	private String filter;     //filter on message'tag
	private Integer mask;    
	
	private String startCopy;  //create group from another group 
	private Long startOffset;
	private String startMsgId; //create group start from offset, msgId to check valid
	private Long startTime;    //create group start from time 
	
	private Integer ackWindow;
	private Long ackTimeout; 
	
	public ConsumeGroup(){
		
	} 
	
	public ConsumeGroup(String groupName){
		this.groupName = groupName;
	} 
	
	public ConsumeGroup(Message msg){ 
		groupName = msg.getConsumeGroup(); 
		groupNameAuto = msg.getGroupNameAuto();
		startCopy = msg.getGroupStartCopy();
		startOffset = msg.getGroupStartOffset();
		startTime = msg.getGroupStartTime();
		startMsgId = msg.getGroupStartMsgId();
		filter = msg.getGroupFilter();
		mask = msg.getGroupMask();    
		ackWindow = msg.getGroupAckWindow();
		ackTimeout = msg.getGroupAckTimeout();
	}
	
	public void writeToMessage(Message msg){
		msg.setConsumeGroup(this.groupName);
		msg.setGroupNameAuto(this.groupNameAuto);
		msg.setGroupStartCopy(this.startCopy);
		msg.setGroupFilter(this.filter);
		msg.setGroupStartMsgId(this.startMsgId);
		msg.setGroupStartOffset(this.startOffset); 
		msg.setGroupStartTime(this.startTime);
		msg.setGroupMask(this.mask); 
		msg.setGroupAckWindow(this.ackWindow);
		msg.setGroupAckTimeout(this.ackTimeout);
	}
	
	public ConsumeGroup asTempBroadcastGroup(){
		this.setGroupName(null);
		this.setGroupNameAuto(true);  //auto generate groupName
		this.setMask(Protocol.MASK_EXCLUSIVE | Protocol.MASK_DELETE_ON_EXIT); //Exclusive + deleteOnExit
		return this;
	}
	
	public static ConsumeGroup createTempBroadcastGroup(){
		return new ConsumeGroup().asTempBroadcastGroup();
	}
	
	public String getStartCopy() {
		return startCopy;
	}
	public void setStartCopy(String groupName) {
		this.startCopy = groupName;
	}
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	} 
	public Long getStartOffset() {
		return startOffset;
	}
	public void setStartOffset(Long startOffset) {
		this.startOffset = startOffset;
	}
	public String getStartMsgId() {
		return startMsgId;
	}
	public void setStartMsgId(String startMsgId) {
		this.startMsgId = startMsgId;
	} 

	public Long getStartTime() {
		return startTime;
	}
	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}
	public String getFilter() {
		return filter;
	} 
	public void setFilter(String filter) {
		this.filter = filter;
	} 
	public Integer getMask() {
		return mask;
	} 
	public void setMask(Integer mask) {
		this.mask = mask;
	} 
	
	public void clearMask() {
		this.mask = null;
	}
	
	public void addMask(Integer value) {
		if(value == null) return;
		if(this.mask == null) {
			this.mask = value;
		}
		
		this.mask |= value;
	}
	
	public void removeMask(Integer value) {
		if(value == null) return;
		if(this.mask == null) {
			return;
		}
		
		this.mask |= ~value;
	} 
	
	public Boolean getGroupNameAuto() {
		return groupNameAuto;
	} 
	public void setGroupNameAuto(Boolean groupNameAuto){
		this.groupNameAuto = groupNameAuto;
	} 
	
	public void setAck(boolean ack) { 
		if(ack) {
			addMask(Protocol.MASK_ACK_REQUIRED);
		} else {
			removeMask(Protocol.MASK_ACK_REQUIRED);
		}
	}
	
	public boolean isAckEnabled(){
		if(mask == null) return false;
		return (mask & Protocol.MASK_ACK_REQUIRED) != 0;
	}
	
	public Integer getAckWindow() {
		return ackWindow;
	}  
	public void setAckWindow(Integer ackWindow) {
		this.ackWindow = ackWindow;
	} 
	public Long getAckTimeout() {
		return ackTimeout;
	} 
	public void setAckTimeout(Long ackTimeout) {
		this.ackTimeout = ackTimeout;
	}
	public void setAckTimeout(long duration, TimeUnit unit) {
		setAckTimeout(unit.toMillis(duration));
	}

	@Override
	public ConsumeGroup clone() { 
		try {
			return (ConsumeGroup)super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}  
}
