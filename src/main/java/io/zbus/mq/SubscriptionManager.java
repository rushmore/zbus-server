package io.zbus.mq;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.mq.model.Subscription;

public class SubscriptionManager {  
	private Map<String, Subscription> clientId2Subscription = new ConcurrentHashMap<>();
	private Map<String, List<Subscription>> channel2Subscription = new ConcurrentHashMap<>();
	
	public synchronized Subscription get(String clientId) {
		return clientId2Subscription.get(clientId);
	}
	
	public synchronized void add(Subscription subscription) {
		clientId2Subscription.put(subscription.clientId, subscription); 
		String channelKey = key(subscription.mq, subscription.channel);
		List<Subscription> subs = channel2Subscription.get(channelKey);
		if(subs == null) {
			subs = new ArrayList<>();
			channel2Subscription.put(channelKey, subs);
		}
		subs.add(subscription);   
	}
	
	
	public synchronized List<Subscription> getSubscriptionList(String mq, String channel){
		String channelKey = key(mq, channel);
		return channel2Subscription.get(channelKey);
	}
	
	public synchronized void removeByClientId(String clientId) { 
		Subscription sub = clientId2Subscription.remove(clientId);
		if(sub == null) return;
		for(List<Subscription> subs : channel2Subscription.values()) {
			subs.remove(sub);
		} 
	}
	
	public synchronized void removeByChannel(String mq, String channel) {
		String channelKey = key(mq, channel);
		List<Subscription> subs = channel2Subscription.remove(channelKey);
		if(subs == null) return;
		
		for(Subscription sub : subs) {
			clientId2Subscription.remove(sub.clientId);
		}
	}
	
	
	public synchronized void removeByMq(String mq) { 
		Iterator<Entry<String, Subscription>> iter = clientId2Subscription.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<String, Subscription> entry = iter.next(); 
			if(mq.equals(entry.getValue().mq)) {
				iter.remove();
			}
		}
		
		Iterator<Entry<String, List<Subscription>>> it = channel2Subscription.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String, List<Subscription>> entry = it.next();
			for(Subscription sub : entry.getValue()) {
				if(mq.equals(sub.mq)) {
					it.remove();
					break;
				}
			}
		}
	}
	
	private String key(String mq, String channel) {
		if(mq == null) mq = "";
		if(channel == null) channel = "";
		String channelKey = mq + "." + channel;
		return channelKey;
	}
}
