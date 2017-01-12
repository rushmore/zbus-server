package io.zbus.mq.disk;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 
 * 000--blockNumber: 8
 * 008--offset: 4 
 * 012--tag: 128
 * 
 * @author Rushmore
 *
 */
public class QueueReader extends MappedFile implements Comparable<QueueReader> {
	private static final int READER_FILE_SIZE = 256;  
	private static final int FITER_TAG_POS = 12;  
	private Block block;  
	private final Index index;  
	private final String readerGroup; 
	 
	private long blockNumber;
	private int offset = 0; 
	private String filterTag; //max: 127 bytes
	
	private String[] filterTagParts;
	private long messageCount = 0; 
	
	private final Lock readLock = new ReentrantLock();  
	
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
		loadMessageCount();
	}   
	
	public QueueReader(QueueReader copy, String readerGroup) throws IOException{
		this.index = copy.index; 
		this.readerGroup = readerGroup;   
		
		load(readerFile(this.readerGroup), READER_FILE_SIZE); 
		
		this.blockNumber = copy.blockNumber;
		this.offset = copy.offset;
		this.messageCount = copy.messageCount;
		
		block = this.index.createReadBlock(this.blockNumber);
	}  
	
	private File readerFile(String readerGroup){
		File readerDir = new File(index.getIndexDir(), Index.ReaderDir);
		return new File(readerDir, this.readerGroup + Index.ReaderSuffix); 
	}
	
	public boolean seek(long offset, String msgid) throws IOException{ 
		return true;
	}   
	
	public boolean seek(long time) throws IOException{ 
		return true;
	}   
	
	public boolean isEOF() throws IOException{
		readLock.lock();
		try{  
			if(block.isEndOfBlock(this.offset)){  
				if(index.overflow(blockNumber+1)){
					return true;
				} 
			} 
			return false;
		} finally {
			readLock.unlock();
		} 
	} 
	
	private void loadMessageCount() throws IOException{
		if(block.isEndOfBlock(this.offset)){  
			if(index.overflow(blockNumber+1)){
				this.messageCount = index.getMessageCount();
				return;
			}
			this.blockNumber++;
			block = this.index.createReadBlock(this.blockNumber);
			this.offset = 0;
		}
		DiskMessage data = block.readHead(offset);
		this.messageCount = data.messageCount-1; 
	}
	
	private DiskMessage readUnsafe(String[] tagParts) throws IOException{
		if(block.isEndOfBlock(this.offset)){  
			if(index.overflow(blockNumber+1)){
				return null;
			}
			this.blockNumber++;
			block = this.index.createReadBlock(this.blockNumber);
			this.offset = 0;
		}
		DiskMessage data = block.readByTag(offset, tagParts);
		this.offset += data.bytesScanned;
		if(data.messageCount > 0){
			this.messageCount = data.messageCount-1;
		}
		
		writeOffset();  
		
		if(!data.valid){
			return readUnsafe(tagParts);
		}
		return data;
	} 
	
	public DiskMessage read() throws IOException{
		readLock.lock();
		try{  
			return readUnsafe(filterTagParts);
		} finally {
			readLock.unlock();
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
			this.filterTag = new String(tag, 1, tagLen);
		}
	}
	
	@Override
	protected void writeDefaultData() throws IOException {
		this.blockNumber = index.getBlockStart();
		this.offset = 0;
		
		writeOffset();
		
		//write tag
		buffer.position(FITER_TAG_POS);
		buffer.put((byte)0); //tag default to null
	}   
	 
	public int getOffset() {
		return offset;
	}  

	public String getFilterTag() {
		return filterTag;
	} 

	public void setFilterTag(String filterTag) {
		readLock.lock();
		try{  
			this.filterTag = filterTag;
			int len = 0;
			if(filterTag != null){
				len = filterTag.length();
				buffer.position(FITER_TAG_POS);
				buffer.put((byte)len); 
				buffer.put(this.filterTag.getBytes());
				
				filterTagParts = this.filterTag.split("[.]");
			} else { //clear
				buffer.position(FITER_TAG_POS);
				buffer.put((byte)0); 
				filterTagParts = null;
			}
		} finally {
			readLock.unlock();
		}  
	} 

	public long getMessageCount() {
		return messageCount;
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
} 
