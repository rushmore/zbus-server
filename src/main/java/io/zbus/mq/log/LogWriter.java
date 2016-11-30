package io.zbus.mq.log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LogWriter { 
	private final Index index;
	private Block writeBlock;
	private final Lock writeLock = new ReentrantLock(); 
	
	public LogWriter(File basePath, String queueName) throws IOException {
		File queueFile = new File(basePath, queueName);
		if (!queueFile.exists()) {
			queueFile.mkdirs();
		} 
		index = new Index(queueFile); 
		writeBlock = index.buildWriteBlock();
	}
	
	public LogWriter(Index index) throws IOException {
		this.index = index;
		writeBlock = index.buildWriteBlock();
	}
	
	public void write(byte[] data) throws IOException{
		writeLock.lock();
		try{
			if(writeBlock.isFull()){
				writeBlock.close();
				writeBlock = index.buildWriteBlock();
			}
			writeBlock.write(data); 
		}
		finally {
			writeLock.unlock();
		}
	}
	
	public Index getIndex() {
		return index;
	}
}
