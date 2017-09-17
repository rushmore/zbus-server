package io.zbus.mq;
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Protocol.ConsumeGroupInfo;
import io.zbus.transport.Session;

public class MemoryQueue extends AbstractQueue { 
	private static final Logger log = LoggerFactory.getLogger(MemoryQueue.class); 
	
	protected Queue<Message> queue = new ConcurrentLinkedQueue<Message>();
	protected int capacity = 1000; 
	protected int mask = 0; 
	protected String creator = "";  
	protected long createdTime = System.currentTimeMillis();  
	
	public MemoryQueue(String topic){
		super(topic);
	}
	
	@Override
	public void produce(Message message) throws IOException {
		queue.offer(message);
		if(queue.size() > capacity){
			queue.poll();
			log.warn("Memory queue full, message discarded");
		}
		
		this.lastUpdatedTime = System.currentTimeMillis(); 
		dispatch();
	}

	@Override
	public Message consume(String consumeGroup) throws IOException {
		return queue.poll();
	} 

	@Override
	public ConsumeGroupInfo declareGroup(ConsumeGroup ctrl) throws Exception {
		String consumeGroup = ctrl.getGroupName();
		if(consumeGroup == null){
			consumeGroup = this.topic;
		}
		
		MemoryConsumeGroup group = (MemoryConsumeGroup)consumeGroups.get(consumeGroup); 
		if(group == null){
			group = new MemoryConsumeGroup(consumeGroup);
			group.filter = ctrl.getFilter();
			group.mask = ctrl.getMask();
			this.consumeGroups.put(consumeGroup, group);
			log.info("ConsumeGroup created: %s", group); 
		}
		return group.getConsumeGroupInfo();
	}
	  
	@Override
	public long createdTime() { 
		return createdTime;
	}
	
	@Override
	public long messageDepth() { 
		return queue.size();
	} 
	 
	@Override
	public String getCreator() { 
		return creator;
	}

	@Override
	public void setCreator(String value) { 
		this.creator = value;
	}

	@Override
	public int getMask() { 
		return mask;
	}

	@Override
	public void setMask(int value) { 
		mask = value;
	}
	 

    class MemoryConsumeGroup extends AbstractConsumeGroup {   
		private String filter;
		private Integer mask;
		private long createdTime = System.currentTimeMillis();
		private long updatedTime = System.currentTimeMillis();
		
		public MemoryConsumeGroup(String groupName) throws IOException {
			super(groupName);
		} 
		
		public void removeSession(Session session){
			pullSessions.remove(session.id());
			Iterator<PullSession> iter = pullQ.iterator();
			while(iter.hasNext()){
				if(iter.next().session == session){
					iter.remove();
					break;
				}
			}
		}
		
		@Override
		public Message read() throws IOException { 
			return queue.poll();
		} 
		
		public boolean isEnd(){
			return queue.size() == 0;
		}
		 
		public ConsumeGroupInfo getConsumeGroupInfo(){
			ConsumeGroupInfo info = new ConsumeGroupInfo(); 
			info.topicName = topic;
			info.filter = filter;
			info.creator = null;
			info.mask = mask == null? 0 : mask;
			info.createdTime = createdTime;
			info.lastUpdatedTime = updatedTime;
			info.consumerCount = pullSessions.size();
			info.messageCount = queue.size();
			info.groupName = groupName;
			info.consumerList = new ArrayList<String>();
			for(Session session : pullSessions.values()){
				info.consumerList.add(session.remoteAddress());
			}
			return info;
		}
	} 
}
