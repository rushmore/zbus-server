package io.zbus.mq.disk;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class QueueReader extends MappedFile {
	public static final int READER_FILE_SIZE = 256;  
	private Block block;  
	private final Index index;  
	private final String readerGroup; 
	
	private int blockNumber = 0;
	private int offset = 0;
	
	private final Lock readLock = new ReentrantLock();  
	
	public QueueReader(Index index, String readerGroup) throws IOException{
		this.index = index; 
		this.readerGroup = readerGroup; 
		File readerDir = new File(index.getIndexDir(), Index.ReaderDir);
		File file = new File(readerDir, this.readerGroup);
		
		load(file, READER_FILE_SIZE); 
		 
		block = this.index.createReadBlock(this.blockNumber);
	}   
	
	public byte[] read() throws IOException{
		readLock.lock();
		try{  
			if(block.isEndOfBlock(this.offset)){ 
				this.blockNumber++;
				if(this.blockNumber >= index.getBlockCount()){
					return null;
				}
				block = this.index.createReadBlock(this.blockNumber);
				this.offset = 0;
			}
			byte[] data = block.read(offset);
			this.offset += 12 + data.length;
			writeOffset(); 
			return data;
		} finally {
			readLock.unlock();
		} 
	} 
	
	@Override
	protected void loadDefaultData() throws IOException {
		buffer.position(0);
		this.blockNumber = buffer.getInt();
		this.offset = buffer.getInt();
	}
	
	@Override
	protected void writeDefaultData() throws IOException {
		this.blockNumber = 0;
		this.offset = 0;
		
		writeOffset();
	}  
	
	private void writeOffset(){
		buffer.position(0); 
		buffer.putInt(blockNumber);
		
		buffer.position(4); 
		buffer.putInt(offset);
	}
} 
