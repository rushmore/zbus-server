package io.zbus.mq.disk;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Block implements Closeable {     
	private volatile int writeOffset = 0; 
	private RandomAccessFile file; 
	private final Index index; 
	private final int blockNumber;
	private boolean isEndOffset = false;
	private final Lock writeLock = new ReentrantLock();  
	 
	Block(Index index, File file, int blockNumber) throws FileNotFoundException{   
		this.index = index;
		this.blockNumber = blockNumber;
		if(this.blockNumber < 0){
			throw new IllegalArgumentException("blockNumber should>=0 but was " + blockNumber);
		}
		if(this.blockNumber >= index.getBlockCount()){
			throw new IllegalArgumentException("blockNumber should<"+index.getBlockCount() + " but was " + blockNumber);
		}
		
		if(!file.exists()){
			File dir = file.getParentFile();
			if(!dir.exists()){
				dir.mkdirs();
			}  
		}  
		
		this.file = new RandomAccessFile(file,"rw");  
		this.writeOffset = this.index.readOffset(this.blockNumber);
		if(this.blockNumber < index.getBlockCount()){
			isEndOffset = true;
		} else {
			isEndOffset = false;
		}
	}   
	 
	
	public void writeSafe(byte[] data) throws IOException{  
		writeLock.lock();
		try {  
			write(data);  
		} finally {
			writeLock.unlock();
		}
	}   
	
	public void write(byte[] data) throws IOException{  
		if(writeOffset > Index.BLOCK_MAX_SIZE){
			throw new IOException("Block full");
		}
		file.seek(writeOffset);
		file.writeLong(writeOffset);
		file.writeInt(data.length);
		file.write(data);
		writeOffset += 8 + 4 + data.length;  
		
		index.writeOffset(writeOffset);
		
		index.newDataAvailable.get().countDown();
		index.newDataAvailable.set(new CountDownLatch(1));
	} 

    public byte[] read(int pos) throws IOException{
		file.seek(pos); 
		file.readLong(); //offset 
		int size = file.readInt();
		byte[] data = new byte[size];
		file.read(data, 0, size);
		return data;
	}
    
    public boolean isFull(){
    	return writeOffset >= Index.BLOCK_MAX_SIZE;
    }
    
    public boolean isReadEnd(int offset){
    	if(index.getBlockCount() > this.blockNumber){
    		if(!isEndOffset){
    			this.writeOffset = index.readOffset(this.blockNumber);
    		}
    		return offset >= this.writeOffset;
    	}
    	return offset >= index.readOffset(this.blockNumber);
    }
	
    public int getBlockNumber() {
		return blockNumber;
	}
    
	@Override
	public void close() throws IOException {  
		this.file.close();
	} 
}
