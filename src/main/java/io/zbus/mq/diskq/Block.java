package io.zbus.mq.diskq;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Block implements Closeable {   
	public static final String BLOCK_FILE_SUFFIX = ".zbus";
	public static final long MaxBlockSize = Long.valueOf(System.getProperty("maxBlockSize", 64*1024*1024+"")); //default to 64M
	
	private volatile int writeOffset = 0; 
	private RandomAccessFile file; 
	 
	private final Lock writeLock = new ReentrantLock(); 
	private final AtomicReference<CountDownLatch> newDataAvailable = new AtomicReference<CountDownLatch>(new CountDownLatch(1));
	
	public Block(File file, int writeOffset) throws FileNotFoundException{   
		if(!file.exists()){
			File dir = file.getParentFile();
			if(!dir.exists()){
				dir.mkdirs();
			}  
		}  
		this.file = new RandomAccessFile(file,"rw");  
		this.writeOffset = writeOffset;
	}   
	
	public Block(File file) throws FileNotFoundException{ 
		this(file, 0);
	}
	
	public void offer(byte[] data) throws IOException{  
		writeLock.lock();
		try {  
			write(data); 
			newDataAvailable.get().countDown();
			newDataAvailable.set(new CountDownLatch(1));
		} finally {
			writeLock.unlock();
		}
	}  
	
	/**
	 * nonthreadsafe
	 * @param data
	 * @throws IOException
	 */
	void write(byte[] data) throws IOException{  
		if(writeOffset > MaxBlockSize){
			throw new IOException("Block full");
		}
		file.seek(writeOffset);
		file.writeLong(writeOffset);
		file.writeInt(data.length);
		file.write(data);
		writeOffset += 8 + 4 + data.length;  
	} 

    byte[] read(long pos) throws IOException{
		file.seek(pos); 
		file.readLong(); //offset 
		int size = file.readInt();
		byte[] data = new byte[size];
		file.read(data, 0, size);
		return data;
	}
    
    public boolean isFull(){
    	return writeOffset >= MaxBlockSize;
    }
	
	@Override
	public void close() throws IOException {  
		this.file.close();
	}
	
	public int getWriteOffset() {
		return writeOffset;
	}
	 
    RandomAccessFile getFile() {
		return file;
	}
	
    AtomicReference<CountDownLatch> getNewDataAvailable() {
		return newDataAvailable;
	}
    
    static void test(String name) throws Exception{
    	
		File file = new File(name);
		final int dataSize = 10240;
		final int count = 6400; 
		
		Block block = new Block(file); 
		byte[] data = new byte[dataSize];
		int writeCount = 0;
		long start = System.currentTimeMillis();
		while(writeCount++ < count){
			if(block.isFull()) break;
			block.offer(data);
		} 
		long end = System.currentTimeMillis();
		block.close(); 
		
		System.out.format("%.2f M/s\n", 1.0*writeCount*dataSize*1000/(end-start)/1024/1024 );
    }
	
	public static void main(String[] args) throws Exception {
		for(int i=0;i<10;i++)
			test("/tmp/test.zbus"+i);
	}
	
}
