package io.zbus.mq;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.mq.db.DbQueue;
import io.zbus.mq.disk.DiskQueue;
import io.zbus.mq.memory.MemoryQueue;
import io.zbus.mq.model.Channel;
import io.zbus.mq.model.MessageQueue;

public class MessageQueueManager {  
	private static final Logger logger = LoggerFactory.getLogger(MessageQueueManager.class);
	public String mqDir = "/tmp/zbus";
	public String dbConnectionString;
	
	private Map<String, MessageQueue> mqTable = new ConcurrentHashMap<>();
	
	public void loadQueueTable() {
		logger.info("Loading MQ from disk ..."); 
		File[] mqDirs = new File(this.mqDir).listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		
		if (mqDirs != null && mqDirs.length > 0) {
			for (File dir : mqDirs) { 
				try {
					MessageQueue mq = new DiskQueue(dir.getName(), new File(this.mqDir));
					mqTable.put(dir.getName(), mq);
					logger.info("MQ({}) loaded", dir.getName()); 
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					continue;
				} 
			}
		}   
	}
	
	public MessageQueue get(String mqName) {
		if(mqName == null) mqName = "";
		return mqTable.get(mqName);
	} 
	
	public MessageQueue saveQueue(String mqName, String channel) throws IOException { 
		return saveQueue(mqName, Protocol.MEMORY, null, channel, null, null);
	}
	
	/**
	 * 
	 * Create MQ or Channel of MQ
	 * 
	 * @param mqName name of message queue
	 * @param mqType type of mq
	 * @param channel channel name of mq
	 * @return created/updated mq
	 * @throws IOException 
	 */
	public MessageQueue saveQueue(
			String mqName, String mqType, Integer mqMask, 
			String channel, Long channelOffset, Integer channelMask) throws IOException { 
		
		if(mqName == null) {
			throw new IllegalArgumentException("Missing mqName");
		}
		if(mqType == null) {
			mqType = Protocol.MEMORY;
		}
		
		MessageQueue mq = mqTable.get(mqName); 
		if(mq == null) {
			if(Protocol.MEMORY.equals(mqType)) {
				mq = new MemoryQueue(mqName);
			} else if (Protocol.DISK.equals(mqType)) {
				mq = new DiskQueue(mqName, new File(mqDir));
			} else if(Protocol.DB.equals(mqName)) {
				mq = new DbQueue(mqName);
			} else {
				throw new IllegalArgumentException("mqType(" + mqType + ") Not Support");
			}  
			mqTable.put(mqName, mq);
		}
		
		mq.setMask(mqMask); 
		
		if(channel != null) {
			Channel ch = new Channel(channel, channelOffset);  
			ch.mask = channelMask;
			mq.saveChannel(ch);
		}
		
		return mq;
	} 
	
	/**
	 * Remove MQ or Channel of MQ
	 * 
	 * @param mq name of mq
	 * @param channel channel of mq
	 */ 
	public void removeQueue(String mq, String channel) throws IOException {
		if(channel == null) {
			MessageQueue q = mqTable.remove(mq);
			if(q != null) {
				q.destroy();
			}
			return;
		}
		
		MessageQueue q = mqTable.get(mq);
		if(q != null) {
			q.removeChannel(channel);
		}
	} 
}
