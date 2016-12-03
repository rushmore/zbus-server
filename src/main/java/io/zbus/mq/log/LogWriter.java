package io.zbus.mq.log;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LogWriter { 
	private final Index index;
	private Block writeBlock;
	private final Lock writeLock = new ReentrantLock();  
	
	public LogWriter(Index index) throws IOException {
		this.index = index;
		writeBlock = index.createWriteBlock();
	}
	
	public void write(byte[] data) throws IOException{
		writeLock.lock();
		try{
			if(writeBlock.isFull()){
				writeBlock.close();
				writeBlock = index.createWriteBlock();
			}
			writeBlock.write(data); 
		}
		finally {
			writeLock.unlock();
		}
	} 
}
