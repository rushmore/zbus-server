package io.zbus.mq.disk.support;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.StrKit;
import io.zbus.mq.disk.support.Index.BlockOffset;
import io.zbus.mq.disk.support.Index.Offset;
 

public class QueueReader extends MappedFile implements Comparable<QueueReader> {
	private static final Logger log = LoggerFactory.getLogger(QueueReader.class); 
	
	private static final int READER_FILE_SIZE = 1024;  
	private static final int FILTER_POS = 12;  
	private static final int FILTER_MAX_LENGTH = 127;  
	private Block block;  
	private final Index index;  
	private final String readerGroup; 
	 
	private long blockNumber;
	private int offset = 0; 
	private String filter; //max: 127 bytes, filter on Messsage's tag
	
	private List<String[]> filterParts = new ArrayList<String[]>();
	private long messageNumber = -1; //the last messageNumber read, the next message number to read is messageNumber+1
	
	private Block randomAccessBlock;
	
	public QueueReader(Index index, String readerGroup) throws IOException{
		this.index = index; 
		this.readerGroup = readerGroup;   
		
		load(readerFile(this.readerGroup), READER_FILE_SIZE); 
		
		if(this.blockNumber < index.getBlockStart()){ //forward to oldest available
			this.blockNumber = index.getBlockStart();
			this.offset = 0;
			writeOffset();
		}
		if(index.overflow(this.blockNumber)){ //backward to latest available
			this.blockNumber = index.currentBlockNumber(); 
			this.offset = index.currentWriteOffset();
		}
		 
		block = this.index.createReadBlock(this.blockNumber);
		readMessageNumber();
	}   
	
	public QueueReader(QueueReader copy, String readerGroup) throws IOException{
		this.index = copy.index; 
		this.readerGroup = readerGroup;     
		
		load(readerFile(this.readerGroup), READER_FILE_SIZE); 
		
		this.blockNumber = copy.blockNumber;
		this.offset = copy.offset;
		this.messageNumber = copy.messageNumber;
		
		if(this.blockNumber < index.getBlockStart()){ //forward to oldest available
			this.blockNumber = index.getBlockStart();
			this.offset = 0; 
		}
		if(index.overflow(this.blockNumber)){ //backward to latest available
			this.blockNumber = index.currentBlockNumber(); 
			this.offset = index.currentWriteOffset();
		}
		
		writeOffset();  
		 
		block = this.index.createReadBlock(this.blockNumber);
	}  
	
	public Index getIndex() {
		return this.index;
	}
	
	public String getGroupName() {
		return this.readerGroup;
	}
	
	public File getReaderDir() {
		return new File(index.getIndexDir(), Index.ReaderDir);
	}
	
	private File readerFile(String readerGroup){
		File readerDir = new File(index.getIndexDir(), Index.ReaderDir);
		return new File(readerDir, this.readerGroup + Index.ReaderSuffix); 
	}
	
	public boolean seek(long totalOffset, String msgid) throws IOException{ 
		BlockOffset res = index.searchBlock(totalOffset);
		if(res == null) {
			return false;
		}
		long blockNo = res.blockNumber;
		Offset offset = res.offset;
		
		int offsetInside = (int)(totalOffset - offset.baseOffset);
		Block b = null;
		if(blockNo == this.blockNumber) {
			b = this.block; 
		} else {
			b = index.createReadBlock(blockNo);
		}
		DiskMessage msg = b.readHead(offsetInside);
		if(!msgid.equals(msg.id)) {
			if(b != this.block) {
				b.close();
			}
			return false;
		}
		
		this.blockNumber = blockNo;
		this.offset = offsetInside;
		if(b != this.block) {
			this.block.close();
		}
		this.block = b; 
		this.messageNumber = msg.messageNumber;
		
		return true;
	}   
	
	public boolean seek(long time) throws IOException{ 
		BlockOffset res = index.searchBlock(offset);
		if(res == null) {
			return false;
		}
		long blockNo = res.blockNumber;
		Offset offset = res.offset;
		 
		Block b = null;
		if(blockNo == this.blockNumber) {
			b = this.block; 
		} else {
			b = index.createReadBlock(blockNo);
		}
		
		int pos = 0;
		
		DiskMessage msg = null;
		while(pos < offset.endOffset) {
			msg = block.readFully(pos);
			pos += msg.bytesScanned;
			if(msg.timestamp<time) continue; 
		}
		if(b != this.block) {
			this.block.close();
		}
		
		if(msg == null) return false;
		
		this.blockNumber = blockNo;
		this.offset = pos; 
		this.block = b; 
		this.messageNumber = msg.messageNumber;
		
		return true;
	}   
	
	public boolean isEOF() throws IOException{
		lock.lock();
		try{  
			if(block.isEndOfBlock(this.offset)){  
				if(index.overflow(blockNumber+1)){
					return true;
				} 
			} 
			return false;
		} finally {
			lock.unlock();
		} 
	} 
	
	private void readMessageNumber() throws IOException{
		if(block.isEndOfBlock(this.offset)){  
			if(index.overflow(blockNumber+1)){
				this.messageNumber = index.getMessageCount()-1;
				return;
			}
			this.blockNumber++;
			block.close();
			block = this.index.createReadBlock(this.blockNumber);
			this.offset = 0;
		}
		DiskMessage data = block.readHead(offset); 
		this.messageNumber = data.messageNumber - 1;  
	}
	
	private DiskMessage readUnsafe(List<String[]> filterParts) throws IOException{
		if(block.isEndOfBlock(this.offset)){  
			if(index.overflow(blockNumber+1)){
				return null;
			}
			this.blockNumber++;
			block.close();
			block = this.index.createReadBlock(this.blockNumber);
			this.offset = 0;
		} 
		DiskMessage data = block.readByFilter(offset, filterParts);
		this.offset += data.bytesScanned;
		this.messageNumber = data.messageNumber == null? -1 : data.messageNumber; 
		
		writeOffset();  
		
		if(!data.valid){
			return readUnsafe(filterParts);
		}
		return data;
	} 
	
	public DiskMessage read() throws IOException{
		lock.lock();
		try{  
			return readUnsafe(filterParts);
		} finally {
			lock.unlock();
		} 
	}
	
	public DiskMessage read(long offset) throws IOException{ 
		
		lock.lock();
		try{  
			BlockOffset blockOffset = index.searchBlock(offset);
			if(blockOffset == null) return null;
			
			if(randomAccessBlock != null && randomAccessBlock.getBlockNumber() != blockOffset.blockNumber) {
				randomAccessBlock.close();
				randomAccessBlock = null;
			}
			if(this.randomAccessBlock == null) {
				this.randomAccessBlock = index.createReadBlock(blockOffset.blockNumber);
			}
			int offsetInside = (int)(offset - blockOffset.offset.baseOffset);
			return randomAccessBlock.readFully(offsetInside);  
		} finally {
			lock.unlock();
		} 
	} 
	
	
	@Override
	protected void loadDefaultData() throws IOException {
		buffer.position(0);
		this.blockNumber = buffer.getLong();
		this.offset = buffer.getInt();    
		
		byte[] tag = new byte[128];
		buffer.get(tag);
		int tagLen = tag[0];
		if(tagLen > 0){
			this.filter = new String(tag, 1, tagLen);
			calcFilter(this.filter); 
		}
	}
	
	@Override
	protected void writeDefaultData() throws IOException {
		this.blockNumber = index.getBlockStart();
		this.offset = 0;
		
		writeOffset();
		
		//write tag
		buffer.position(FILTER_POS);
		buffer.put((byte)0); //tag default to null
	}   
	 
	public int getOffset() {  
		return offset;
	}  
	
	public Long getTotalOffset() {  
		return block.getBaseOffset() + offset;
	}  

	public String getFilter() {
		return filter;
	} 

	public void setFilter(String filter) {
		if(filter != null && filter.length() > FILTER_MAX_LENGTH){
			log.warn("filter:[" + filter + "] exceed max length 127");
			return;
		} 
		lock.lock();
		try{  
			this.filter = filter;
			int len = 0;
			if(StrKit.isEmpty(filter)){ //clear
				buffer.position(FILTER_POS);
				buffer.put((byte)0);   
			} else {
				len = filter.length();
				buffer.position(FILTER_POS);
				buffer.put((byte)len); 
				buffer.put(this.filter.getBytes());
				
				calcFilter(filter);
			}
		} finally {
			lock.unlock();
		}  
	} 
	
	private void calcFilter(String filter){
		filterParts.clear();
		for(String filterGroup : this.filter.split("[;]")){
			if(StrKit.isEmpty(filterGroup)){
				continue;
			}
			String[] parts = filterGroup.split("[.]");
			filterParts.add(parts);
		} 
	}

	public long getMessageNumber() {
		return messageNumber;
	}
	
	public long getMessageCount(){
		return index.getMessageCount() - messageNumber-1;
	}

	private void writeOffset(){
		buffer.position(0); 
		buffer.putLong(blockNumber); 
		buffer.putInt(offset);
	} 

	@Override
	public int compareTo(QueueReader o) { 
		if(this.blockNumber < o.blockNumber) return -1;
		if(this.blockNumber > o.blockNumber) return 1;
		return this.offset-o.offset;
	}
	
	@Override
	public void close() throws IOException { 
		super.close();
		if(block != null){
			block.close();
		}
		if(randomAccessBlock != null) {
			randomAccessBlock.close();
		}
	}
	
	public String getIndexName(){
		return this.index.getName();
	}
} 
