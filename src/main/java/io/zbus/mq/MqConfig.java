package io.zbus.mq;

import io.zbus.mq.Broker.ServerSelector;

public class MqConfig implements Cloneable { 
	protected Broker broker;  
	protected ServerSelector adminServerSelector; //default to null 
	 
	protected String token;   
	protected int invokeTimeout = 10000;  // 10 s 
	protected Integer topicMask;
	
	protected boolean verbose = false; 
	
	public MqConfig(){
		
	}
	
	public MqConfig(Broker broker){
		this.broker = broker;
	}
	
	public Broker getBroker() {
		return broker;
	}

	public void setBroker(Broker broker) {
		this.broker = broker;
	}
 
	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	} 
	 
	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	} 
	
	public int getInvokeTimeout() {
		return invokeTimeout;
	}

	public void setInvokeTimeout(int invokeTimeout) {
		this.invokeTimeout = invokeTimeout;
	}  
	
	public Integer getTopicMask() {
		return topicMask;
	}

	public void setTopicMask(Integer topicMask) {
		this.topicMask = topicMask;
	}

	public ServerSelector getAdminServerSelector() {
		return adminServerSelector;
	}

	public void setAdminServerSelector(ServerSelector adminServerSelector) {
		this.adminServerSelector = adminServerSelector;
	}

	@Override
	public MqConfig clone() { 
		try {
			return (MqConfig)super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
}
