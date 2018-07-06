package io.zbus.mq.plugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * URL mapping to MQ, when mq is missing in request message.
 * 
 * If no mapping is provided, /{mq}/xx/... format of url is assume, and the first part of url is
 * assumed to be mq
 * 
 * @author leiming.hong Jul 3, 2018
 *
 */

public class UrlMapping {  
	private Map<String, List<UrlEntry>> mq2urlList = new ConcurrentHashMap<>();
	private Map<String, String> url2mq = new ConcurrentHashMap<>(); 
	
	public String match(String url) { //TODO speed up url
		int length = 0;
		String mq = null;
		Iterator<Entry<String, String>> iter = url2mq.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<String, String> e = iter.next();
			String key = e.getKey();
			if(url.startsWith(key)) {
				if(key.length() > length) {
					length = key.length();
					mq = e.getValue();
				}
			}
		} 
		return mq; 
	} 
	
	public void clear(String mq) {
		List<UrlEntry> urlEntries = mq2urlList.remove(mq);
		if(urlEntries == null) return;
		
		for(UrlEntry e : urlEntries) {
			url2mq.remove(e.url);
		}
	}
	
	public void addMapping(UrlEntry entry) {
		url2mq.put(entry.url, entry.mq);
		List<UrlEntry> entryList = mq2urlList.get(entry.mq);
		if(entryList == null) {
			entryList = new ArrayList<>();
			mq2urlList.put(entry.mq, entryList);
		}
		
		if(!entryList.contains(entry)) {
			entryList.add(entry);
		} 
	}
	
	public void addMapping(List<UrlEntry> entryList) {
		for(UrlEntry entry : entryList) {
			addMapping(entry);
		} 
	}  
}
