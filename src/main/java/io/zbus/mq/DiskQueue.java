package io.zbus.mq;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Protocol.ConsumeGroupInfo;
import io.zbus.mq.disk.DiskMessage;
import io.zbus.mq.disk.Index;
import io.zbus.mq.disk.QueueNak;
import io.zbus.mq.disk.QueueReader;
import io.zbus.mq.disk.QueueWriter;
import io.zbus.mq.disk.QueueNak.TimeoutMessage;
import io.zbus.transport.Session;


public class DiskQueue extends AbstractQueue{
	private static final Logger log = LoggerFactory.getLogger(DiskQueue.class); 
	protected final Index index;     
	protected final QueueWriter writer; 
	
	public DiskQueue(File dir) throws IOException {   
		this.index = new Index(dir);
		this.topic = index.getName();
		this.writer = new QueueWriter(this.index);
		loadConsumeGroups();
	}
	
	protected void loadConsumeGroups() throws IOException{ 
        File[] readerFiles = index.getReaderDir().listFiles(new FileFilter() { 
			@Override
			public boolean accept(File pathname) {
				return Index.isReaderFile(pathname);
			}
		});
        if (readerFiles != null && readerFiles.length> 0) {
            for (File readerFile : readerFiles) {  
            	String groupName = readerFile.getName();
            	groupName = groupName.substring(0, groupName.lastIndexOf('.'));
            	
            	DiskConsumeGroup group = new DiskConsumeGroup(groupName);
            	consumeGroups.put(groupName, group);
            }
        } 
	}
	 
	  
	public ConsumeGroupInfo declareGroup(ConsumeGroup ctrl) throws Exception{
		String consumeGroup = ctrl.getGroupName();
		if(consumeGroup == null || (ctrl.getGroupNameAuto() != null && ctrl.getGroupNameAuto()==true)){ 
			consumeGroup = nextGroupName();
		}
		Integer mask = ctrl.getMask();
		DiskConsumeGroup group = (DiskConsumeGroup)consumeGroups.get(consumeGroup); 
		if(group == null){
			QueueReader copyReader = null;
			//1) copy reader from base group 
			if(ctrl.getStartCopy() != null){
				DiskConsumeGroup copyGroup = (DiskConsumeGroup)consumeGroups.get(ctrl.getStartCopy());
				if(copyGroup != null){
					copyReader = copyGroup.reader; 
				}
			}
			//2) default to copy latest one
			if(copyReader == null){ 
				copyReader = findLatestReader(); 
			}  
			
			if(copyReader != null){ 
				group = new DiskConsumeGroup(consumeGroup, copyReader);  
			} else {  
				//3) consume from the very beginning
				group = new DiskConsumeGroup(consumeGroup);  
			}    
			consumeGroups.put(consumeGroup, group); 
			log.info("ConsumeGroup created: %s", group); 
		} 
		group.setFilter(ctrl.getFilter()); 
		
		if(mask != null){
			group.setMask(mask);
		}
		Integer ackWindow = ctrl.getAckWindow();
		Long ackTimeout = ctrl.getAckTimeout();
		
		if(ackWindow != null) {
			group.setAckWindow(ackWindow);
		}
		if(ackTimeout != null) { 
			group.setAckTimeout(ackTimeout);
		}
		
		if(ctrl.getStartOffset() != null){
			boolean seekOk = group.seek(ctrl.getStartOffset(), ctrl.getStartMsgId());
			if(!seekOk){
				String errorMsg = String.format("seek by offset unsuccessfull: (offset=%d, msgid=%s)", ctrl.getStartOffset(), ctrl.getStartMsgId());
				throw new IllegalArgumentException(errorMsg);
			}
		} else { 
			if(ctrl.getStartTime() != null){
				boolean seekOk = group.seek(ctrl.getStartTime());
				if(!seekOk){
					String errorMsg = String.format("seek by time unsuccessfull: (time=%d)", ctrl.getStartTime());
					throw new IllegalArgumentException(errorMsg);
				}
			}
		}
		return group.getConsumeGroupInfo();
	}   
	
	private QueueReader findLatestReader(){ 
		if(consumeGroups.isEmpty()) return null; 
		
		List<DiskConsumeGroup> readerList = new ArrayList<DiskConsumeGroup>();
		for(AbstractConsumeGroup group : consumeGroups.values()){
			readerList.add((DiskConsumeGroup)group);
		}
		
		Collections.sort(readerList, new Comparator<DiskConsumeGroup>() { 
			@Override
			public int compare(DiskConsumeGroup o1, DiskConsumeGroup o2) {
				return - o1.reader.compareTo(o2.reader);
			}
		}); 
		return readerList.get(0).reader;
	}
	 
	  
	@Override
	public void destroy() throws IOException { 
		for(AbstractConsumeGroup group : consumeGroups.values()){
			group.delete();
		}
		writer.close();
		index.delete(); 
	}
	
	@Override
	public void produce(Message msg) throws IOException{ 
		DiskMessage data = new DiskMessage();
		data.id = msg.getId();
		data.tag = msg.getTag(); 

		data.body = msg.toBytes(); 
		writer.write(data); 
		this.lastUpdatedTime = System.currentTimeMillis(); 
		dispatch();
	}
	
	@Override
	public Message consume(String consumeGroup) throws IOException {
		DiskConsumeGroup group = (DiskConsumeGroup)consumeGroups.get(consumeGroup);
		if(group == null){
			throw new IllegalArgumentException("ConsumeGroup("+consumeGroup+") not found");
		}   
		return group.read();
	}  
	
	@Override
	public String getCreator() {
		return index.getCreator();
	}

	@Override
	public void setCreator(String value) { 
		index.setCreator(value);
	} 
	
	@Override
	public int getMask() {
		return index.getMask();
	}
	@Override
	public void setMask(int value) {
		index.setMask(value);
	} 
	
	@Override
	public long createdTime() {
		return index.getCreatedTime();
	}
	
	@Override
	public long messageDepth() { 
		return index.getMessageCount();  
	} 
	 
	
	private class DiskConsumeGroup extends AbstractConsumeGroup { 
		private final QueueReader reader; 
		private QueueNak queueNak = null;
		
		public DiskConsumeGroup(String groupName) throws IOException{ 
			super(groupName);
			reader = new QueueReader(index, this.groupName);
			
			initNakQueue();
		}
		
		public DiskConsumeGroup(String groupName, QueueReader reader) throws IOException{ 
			super(groupName);
			this.reader = new QueueReader(reader, groupName);
			
			initNakQueue();
		}
		
		@Override
		public boolean isNakFull() { 
			if(queueNak == null) return false;
			return queueNak.remaining() <= 0;
		}
		
		private void initNakQueue() throws IOException {
			if(queueNak != null) return;
			Integer mask = reader.getMask();
			if(mask == null || mask == 0) {
				mask = index.getMask(); //use index if missing
			}
			
			if(mask != null && (mask&Protocol.MASK_ACK_REQUIRED) != 0) {  
				queueNak = new QueueNak(reader);
			}
		}
		
		@Override
		public void ack(long offset) throws IOException {
			if(queueNak == null) return;
			
			queueNak.removeNak(offset);
		}
		
		public void setAckWindow(Integer window) {
			if(queueNak == null) return;
			queueNak.setWindow(window);
		}
		
		public void setAckTimeout(Long timeout) {
			if(queueNak == null) return;
			queueNak.setTimeout(timeout);
		}
		
		@Override
		public boolean isEnd() { 
			try {
				return reader.isEOF();
			} catch (IOException e) {
				return true;
			}
		}
		
		@Override
		public Message readTimeoutMessage() throws IOException {
			TimeoutMessage data = null; 
			if(queueNak != null) {  
				data = queueNak.pollTimeoutMessage();
			}
			if(data == null) return null;
			Message message = convert(data.diskMessage);
			message.setRetry(data.nakRecord.retryCount);
			return message;
		}
		
		@Override
		public Message read() throws IOException { 
			DiskMessage data = reader.read(); 
			
			if(data == null){
				return null;
			}
			
			return convert(data);
		} 
		
		@Override
		public Message read(long offset) throws IOException {
			DiskMessage data = reader.read(offset);
			if(data == null) {
				return null;
			}
			return convert(data);
		}
		
		private Message convert(DiskMessage data) {
			Message msg = Message.parse(data.body);
			if(msg == null){ 
				log.warn("data read from queue can not be serialized back to Message type");
			} else {
				msg.setOffset(data.offset);  
				msg.setTimestamp(data.timestamp);
			}
			return msg;
		}
		
		public boolean seek(long totalOffset, String msgid) throws IOException{ 
			return reader.seek(totalOffset, msgid);
		}
		
		public boolean seek(long time) throws IOException { 
			return reader.seek(time);
		}
		
		public void setFilter(String filter) {
			reader.setFilter(filter);
		} 
		
		@Override
		public void close() throws IOException {
			reader.close(); 
			if(queueNak != null) {
				queueNak.close();
			}
		} 
		
		public void delete() throws IOException{
			if(queueNak != null) { 
				queueNak.delete();
			}
			reader.delete();
		}
		
		public Integer getMask(){
			Integer mask = reader.getMask();
			if(mask == null || mask == 0) {
				mask = index.getMask(); //use Topic mask instead if not set
			}
			return mask;
		}
		
		public void setMask(Integer mask) throws IOException {
			reader.setMask(mask);
			
			initNakQueue();
		}
		
		@Override
		public void recordNak(Long offset, Integer retryCount) {
			 if(queueNak != null && offset != null) {
				 queueNak.addNak(offset, retryCount);
			 } 
		}
		
		public ConsumeGroupInfo getConsumeGroupInfo(){
			ConsumeGroupInfo info = new ConsumeGroupInfo(); 
			info.topicName = reader.getIndexName();
			info.filter = reader.getFilter();
			info.creator = reader.getCreator();
			info.mask = reader.getMask();
			info.createdTime = reader.getCreatedTime();
			info.lastUpdatedTime = reader.getUpdatedTime();
			info.consumerCount = pullSessions.size();
			info.messageCount = reader.getMessageCount();
			info.groupName = groupName;
			info.consumerList = new ArrayList<String>();
			for(Session session : pullSessions.values()){
				info.consumerList.add(session.remoteAddress());
			}
			return info;
		}
	} 
}