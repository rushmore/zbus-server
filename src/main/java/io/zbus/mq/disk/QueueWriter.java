package io.zbus.mq.disk;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
 
public class QueueWriter implements Closeable{ 
	private final Index index;
	private Block writeBlock;
	private final Lock writeLock = new ReentrantLock();  
	
	public QueueWriter(Index index) throws IOException {
		this.index = index;
		writeBlock = index.createWriteBlock();
	}
	 
	public void write(DiskMessage... data) throws IOException{
		if(data.length == 0) return;
		
		writeLock.lock();
		try{ 
			int count = writeBlock.write(data);
			if(count <= 0){
				writeBlock.close();
				writeBlock = index.createWriteBlock(); 
				writeBlock.write(data);
			}
		}
		finally {
			writeLock.unlock();
		}
	} 
	
	@Override
	public void close() throws IOException {  
		if(writeBlock != null){
			writeBlock.close();
		}
	}
}
