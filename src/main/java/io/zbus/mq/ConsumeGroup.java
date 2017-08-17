package io.zbus.mq;

public class ConsumeGroup implements Cloneable { 
	private String groupName;
	private String filter;     //filter on message'tag
	private Integer mask;   
	
	private String startCopy;  //create group from another group 
	private Long startOffset;
	private String startMsgId; //create group start from offset, msgId to check valid
	private Long startTime;    //create group start from time
	
	//only used in server side, TODO
	private String creator;
	
	public ConsumeGroup(){
		
	} 
	
	public ConsumeGroup(String groupName){
		this.groupName = groupName;
	} 
	
	public ConsumeGroup(Message msg){ 
		groupName = msg.getConsumeGroup();
		startCopy = msg.getGroupStartCopy();
		startOffset = msg.getGroupStartOffset();
		startTime = msg.getGroupStartTime();
		startMsgId = msg.getGroupStartMsgId();
		filter = msg.getGroupFilter();
		mask = msg.getGroupMask();
		creator = msg.getToken(); //token as creator
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
	public String getCreator() {
		return creator;
	} 
	public void setCreator(String creator) {
		this.creator = creator;
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
