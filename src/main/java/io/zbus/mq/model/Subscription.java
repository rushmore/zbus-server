package io.zbus.mq.model;

import java.util.HashSet;
import java.util.Set;

public class Subscription {
	public Set<String> filters = new HashSet<>();
	public String mq;
	public String channel;
	public String clientId; 
	public Integer window; 
	
	public void setFilter(String filter) {
		this.filters.clear();
		this.filters.add(filter);
	}
}
