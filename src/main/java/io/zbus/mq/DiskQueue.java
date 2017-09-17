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
import io.zbus.mq.disk.QueueReader;
import io.zbus.mq.disk.QueueWriter;
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
            	
            	DiskConsumeGroup group = new DiskConsumeGroup(this.index, groupName);
            	consumeGroups.put(groupName, group);
            }
        } 
	}
	  
	public ConsumeGroupInfo declareGroup(ConsumeGroup ctrl) throws Exception{
		String consumeGroup = ctrl.getGroupName();
		if(consumeGroup == null){
			consumeGroup = this.topic;
		}
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
				group = new DiskConsumeGroup(copyReader, consumeGroup);  
			} else {  
				//3) consume from the very beginning
				group = new DiskConsumeGroup(this.index, consumeGroup);  
			}   
			String creator = ctrl.getCreator();
			if(creator != null){
				group.reader.setCreator(ctrl.getCreator());
			}
			consumeGroups.put(consumeGroup, group); 
			log.info("ConsumeGroup created: %s", group); 
		} 
		group.reader.setFilter(ctrl.getFilter());
		Integer mask = ctrl.getMask();
		if(mask != null){
			group.reader.setMask(mask);
		}
		
		if(ctrl.getStartOffset() != null){
			boolean seekOk = group.reader.seek(ctrl.getStartOffset(), ctrl.getStartMsgId());
			if(!seekOk){
				String errorMsg = String.format("seek by offset unsuccessfull: (offset=%d, msgid=%s)", ctrl.getStartOffset(), ctrl.getStartMsgId());
				throw new IllegalArgumentException(errorMsg);
			}
		} else { 
			if(ctrl.getStartTime() != null){
				boolean seekOk = group.reader.seek(ctrl.getStartTime());
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
	 
	
	private class DiskConsumeGroup extends AbstractConsumeGroup{ 
		public final QueueReader reader; 
		
		public DiskConsumeGroup(Index index, String groupName) throws IOException{ 
			super(groupName);
			reader = new QueueReader(index, this.groupName);
		}
		
		public DiskConsumeGroup(QueueReader reader, String groupName) throws IOException{ 
			super(groupName);
			this.reader = new QueueReader(reader, groupName);
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
		public Message read() throws IOException { 
			DiskMessage data = reader.read();
			if(data == null){
				return null;
			}
			
			Message msg = Message.parse(data.body);
			if(msg == null){ 
				log.warn("data read from queue can not be serialized back to Message type");
			} else {
				msg.setOffset(data.offset);   
			}
			return msg;
		}
		
		@Override
		public void close() throws IOException {
			reader.close(); 
		} 
		
		public void delete() throws IOException{
			reader.delete();
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