package io.zbus.mq;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.mq.model.MessageQueue;
import io.zbus.mq.model.Subscription;
import io.zbus.transport.Message;
import io.zbus.transport.Session;

public class MessageDispatcher {
	private static final Logger logger = LoggerFactory.getLogger(MessageDispatcher.class);

	private SubscriptionManager subscriptionManager;
	private Map<String, Session> sessionTable;
	private Map<String, Long> loadbalanceTable = new ConcurrentHashMap<String, Long>(); // channel => index

	//private int batchReadSize = 10;
	private ExecutorService dispatchRunner = Executors.newFixedThreadPool(64);

	public MessageDispatcher(SubscriptionManager subscriptionManager, Map<String, Session> sessionTable) {
		this.subscriptionManager = subscriptionManager;
		this.sessionTable = sessionTable;
	}

	public void dispatch(MessageQueue mq, String channel) {  
		dispatchRunner.submit(() -> { 
			dispatch0(mq, channel);
		});
	}
 
	protected void dispatch0(MessageQueue mq, String channel) {
		List<Subscription> subs = subscriptionManager.getSubscriptionList(mq.name(), channel);
		if (subs == null || subs.size() == 0)
			return;

		synchronized (subs) { 
			Long index = loadbalanceTable.get(channel);
			if (index == null) {
				index = 0L;
			}
			
			while(true) {
				Message message = null;
				int N = subs.size();
				long max = index + N;
				if(max < 0) {
					index = 0L;
					max = index + N;
				}
				boolean windowOpen = false;
				while (index < max) {
					Subscription sub = subs.get((int) (index % N)); 
					
					if(sub.window != null && sub.window <= 0) { 
						loadbalanceTable.put(channel, ++index); 
						continue;
					} 
					windowOpen = true;
					
					if(message == null) {
						try {
							message = mq.read(channel);
						} catch (IOException e) {
							logger.error(e.getMessage(), e);
							break;
						} 
					}
					if(message == null) return;
					
					loadbalanceTable.put(channel, ++index); 
					String filter = (String) message.getHeader(Protocol.TAG);
					if (sub.filters.isEmpty() || sub.filters.contains(filter)) {
						Session sess = sessionTable.get(sub.clientId);
						if (sess == null) continue;
						
						message.setHeader(Protocol.CHANNEL, channel); 
						if(sub.window != null) {
							message.setHeader(Protocol.WINDOW, --sub.window);
						}
						sess.write(message);  
						message = null;
					} 
				} 
				if(!windowOpen) break;
			}  
		}
	}

	public void dispatch(MessageQueue mq) {
		Iterator<String> iter = mq.channelIterator();
		while(iter.hasNext()) {
			String channel = iter.next();
			dispatch(mq, channel);
		} 
	}

	public void take(MessageQueue mq, String channel, int count, String reqMsgId, Session sess) throws IOException { 
		List<Message> data = mq.read(channel, count);
		Message message = new Message();  
		int status = data.size()>0? 200 : 604;//Special status code, no DATA
		message.setStatus(status); 
		message.setHeader(Protocol.ID, reqMsgId);
		message.setHeader(Protocol.MQ, mq.name());
		message.setHeader(Protocol.CHANNEL, channel);   
		message.setBody(data); 
		
		
		sess.write(message); 
	} 
}
