package io.zbus.mq.log;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LogReader {
	private Block block;
	private long readOffset = 0;
	private final AtomicReference<CountDownLatch> newDataAvailable;
	private final Lock readLock = new ReentrantLock();
	
	public LogReader(Block block){
		this.block = block;
		this.newDataAvailable = block.getNewDataAvailable();
	}
	
	public byte[] read() throws IOException, InterruptedException{  
		readLock.lock();
		byte[] data = null; 
		try {    
			while(readOffset >= block.getWriteOffset()){
				if(block.isFull()) return null;
				newDataAvailable.get().await(); 
			} 
			data = block.read(readOffset);  
			readOffset += 8 + 4 + data.length;
			return data;
		} finally {
			readLock.unlock();
		}  
	}  
}
