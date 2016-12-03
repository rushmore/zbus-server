package io.zbus.mq.log;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Block implements Closeable {     
	private volatile int writeOffset = 0; 
	private RandomAccessFile file; 
	private final Index index; 
	private final Lock writeLock = new ReentrantLock();  
	
	/**
	 * TODO add File parameter: sequential number
	 * @param index
	 * @param file
	 * @throws FileNotFoundException
	 */
	public Block(Index index, File file) throws FileNotFoundException{   
		this.index = index;
		if(!file.exists()){
			File dir = file.getParentFile();
			if(!dir.exists()){
				dir.mkdirs();
			}  
		}  
		this.file = new RandomAccessFile(file,"rw");  
		this.writeOffset = this.index.readOffset();
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
    	return offset >= writeOffset;
    }
	
	@Override
	public void close() throws IOException {  
		this.file.close();
	} 
}
