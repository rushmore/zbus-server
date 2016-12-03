package io.zbus.mq.log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LogReader extends MappedFile {
	public static final int READER_FILE_SIZE = 256;  
	private Block block;  
	private final Index index;  
	private final String readerGroup; 
	
	private int blockIndex = 0;
	private int offset = 0;
	
	private final Lock readLock = new ReentrantLock();  
	
	public LogReader(Index index, String readerGroup) throws IOException{
		this.index = index; 
		this.readerGroup = readerGroup; 
		File readerDir = new File(index.getIndexDir(), Index.DIR_READER);
		File file = new File(readerDir, this.readerGroup);
		
		load(file, READER_FILE_SIZE); 
		 
		block = this.index.createReadBlock(this.blockIndex);
	}   
	
	public byte[] read() throws IOException{
		readLock.lock();
		try{ 
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
		this.blockIndex = buffer.getInt();
		this.offset = buffer.getInt();
	}
	
	@Override
	protected void writeDefaultData() throws IOException {
		this.blockIndex = 0;
		this.offset = 0;
		
		buffer.position(0); 
		buffer.putInt(blockIndex);
		writeOffset();
	}  
	
	private void writeOffset(){
		buffer.position(4); 
		buffer.putInt(offset);
	}
} 
