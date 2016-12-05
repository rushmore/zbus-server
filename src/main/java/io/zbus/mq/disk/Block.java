package io.zbus.mq.disk;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.CountDownLatch;

/**
 * Non-threadsafe
 * 
 * @author Rushmore 
 */
class Block implements Closeable {  
	private final Index index; 
	private final int blockNumber;
	private volatile int endOffset = 0; 
	
	private RandomAccessFile diskFile;  
	
	Block(Index index, File file, int blockNumber) throws IOException{   
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
		
		this.diskFile = new RandomAccessFile(file,"rw");  
		this.endOffset = index.readOffset(this.blockNumber).endOffset; 
	}   
	
	
	
	public int write(byte[] data) throws IOException{
		if(endOffset >= Index.BlockMaxSize){
			return 0;
		}
		diskFile.seek(endOffset);
		diskFile.writeLong(endOffset);
		diskFile.writeInt(data.length);
		diskFile.write(data);
		endOffset += 8 + 4 + data.length;  
		
		index.writeEndOffset(endOffset);
		
		index.newDataAvailable.get().countDown();
		index.newDataAvailable.set(new CountDownLatch(1));
		return data.length;
	}  

	
    public byte[] read(int pos) throws IOException{
		diskFile.seek(pos); 
		diskFile.readLong(); //offset 
		int size = diskFile.readInt();
		byte[] data = new byte[size];
		diskFile.read(data, 0, size);
		return data;
	}
    
    /**
     * Check if endOffset of block reached max block size allowed
     * @return true if max block size reached, false other wise
     */
    public boolean isFull(){
    	return endOffset >= Index.BlockMaxSize;
    }
    
    /**
     * Check if offset reached the end, for read.
     * @param offset offset of reading
     * @return true if reached the end of block(available data), false otherwise
     * @throws IOException 
     */
    public boolean isEndOfBlock(int offset) throws IOException{  
    	return offset >= index.readOffset(blockNumber).endOffset;
    }
	
    public int getBlockNumber() {
		return blockNumber;
	}
    
	@Override
	public void close() throws IOException {  
		this.diskFile.close();
	} 
}
