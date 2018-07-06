package io.zbus.mq.plugin;

import java.util.List;
import java.util.Map.Entry;

import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.mq.MessageQueueManager;
import io.zbus.mq.Protocol;
import io.zbus.transport.Message;
import io.zbus.transport.http.Http;

public class DefaultUrlFilter implements UrlFilter {
	private UrlMapping urlMapping = new UrlMapping();
	private MessageQueueManager mqManager;
	
	public DefaultUrlFilter(MessageQueueManager mqManager) {
		this.mqManager = mqManager;
	}
	
	@Override
	public Message doFilter(Message req) { 
		String url = req.getUrl();
		if(url == null) return null;  
		
		//CMD or MQ populated in header, use header control, no need parse URL
		if(req.getHeader(Protocol.CMD) != null || req.getHeader(Protocol.MQ) != null) {
			return null;
		} 
		
		String mq = urlMapping.match(url); 
		
		UrlInfo info = HttpKit.parseUrl(url);
		
		if(info.pathList.size()==0) { 
			if(mq != null) {
				req.setHeader(Protocol.MQ, mq);
			} 
			
			for(Entry<String, String> e : info.queryParamMap.entrySet()) {
				String key = e.getKey();
				Object value = e.getValue();
				if(key.equals("body")) {
					req.setBody(value);
					continue;
				}
				req.setHeader(key.toLowerCase(), value);
			}   
			if(mq == null) {
				return null; 
			}
		}
		
		if(mq == null) { 
			if(info.pathList.size() > 0) {
				mq = info.pathList.get(0); 
			}
		} 
		
		if(mqManager.get(mq) == null) {
			Message res = new Message();
			res.setStatus(404);
			res.setHeader(Http.CONTENT_TYPE, "text/html; charset=utf8");
			res.setBody(String.format("%s Not Found", url));
			return res;
		}
		
		//Assumed to be RPC
		if(req.getHeader(Protocol.CMD) == null) { // RPC assumed
			req.setHeader(Protocol.CMD, Protocol.PUB);
			req.setHeader(Protocol.ACK, false); //ACK should be disabled
		}     
		
		if(mq != null) {
			req.setHeader(Protocol.MQ, mq);
		}
		return null;
	}
	
	@Override
	public void updateUrlEntry(String mq, List<UrlEntry> entries, boolean clear) {
		for(UrlEntry e : entries) {
			if(!mq.equals(e.mq)) {
				throw new IllegalArgumentException("UrlEntry's mq should be same");
			} 
		}
		
		if(clear) urlMapping.clear(mq); 
		urlMapping.addMapping(entries); 
	}
}
