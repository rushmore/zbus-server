package io.zbus.mq.disk;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * --[4] OffsetCount 
 * --[~1024] Extension 
 * --[24 bytes] -- Offset 
 * 
 * @author Rushmore
 *
 */

public class Index extends MappedFile {
	public static final String IndexSuffix = ".idx";  
	public static final String BlockSuffix = ".zbus";  
	public static final String BlockDir    = "data"; 
	public static final String ReaderDir   = "reader"; 
	
	public static final int  BlockMaxCount = 10240; 
	public static final long BlockMaxSize  = 64*1024*1024; //default to 64M
	
	public static final int OffsetSize     = 20;
	public static final int IndexHeadSize  = 1024;  
	public static final int IndexSize      = IndexHeadSize + BlockMaxCount * OffsetSize;  
	
	 
	private volatile int blockCount = 0;  
	public final AtomicReference<CountDownLatch> newDataAvailable = new AtomicReference<CountDownLatch>(new CountDownLatch(1));; 
	  
	private File indexDir;  
	
	public Index(File dir) { 
		this.indexDir = dir;   
		File file = new File(indexDir, this.indexDir.getName() + IndexSuffix); 
		load(file, IndexSize); 
	}  
	
	/**
	 * Write endOffset of last block 
	 * @param endOffset writable offset of last block
	 * @throws IOException 
	 */
	public void writeEndOffset(int endOffset) throws IOException{  
		FileLock lock = fileChannel.lock();
		try {
			buffer.position(IndexHeadSize + (blockCount-1)*OffsetSize + 16);
			buffer.putInt(endOffset);
		} finally {
			lock.release();
		} 
	}
	
	public int readEndOffset() throws IOException{
		FileLock lock = fileChannel.lock();
		try {
			buffer.position(IndexHeadSize + (blockCount-1)*OffsetSize + 16);
			return buffer.getInt();
		} finally {
			lock.release();
		}
	} 
	
	public Block createWriteBlock() throws IOException{
		if(blockCount < 1 || isLastBlockFull()){
			return createBlock();
		}
		
		Offset offset = getOffset(blockCount-1);
		Block block = new Block(this, blockFile(offset.baseOffset), blockCount-1); 
		return block;
	}
	
	public Block createReadBlock(int blockNumber) throws IOException{
		if(blockCount < 1){
			throw new IllegalStateException("No block to read");
		}
		checkBlockNumber(blockNumber);
		
		Offset offset = getOffset(blockNumber);
		Block block = new Block(this, blockFile(offset.baseOffset), blockNumber); 
		return block;
	}  
	
	public Offset getOffset(int blockNumber) {
		checkBlockNumber(blockNumber);
		
		buffer.position(IndexHeadSize + blockNumber * OffsetSize);
		
		Offset offset = new Offset();
		offset.createdTime = buffer.getLong();
		offset.baseOffset = buffer.getLong(); 
		offset.endOffset = buffer.getInt(); 
		return offset;
	}
	
	private void checkBlockNumber(int blockNumber){ 
		if(blockNumber < 0 ||  blockNumber >= blockCount){
			throw new IllegalArgumentException("blockNumber should >=0 and <"+blockCount + ", but was " + blockNumber);
		}
	}
	
	/**
	 * Search block number by totalOffset
	 * @param readOffset offset from block 0, not the offset in the block.
	 * @return block number the tottalOffset follows.
	 * @throws IOException
	 */
	public int searchBlockNumber(long totalOffset) throws IOException{
		for(int i=0; i<blockCount; i++){
			Offset offset = getOffset(i);
			if(totalOffset >= offset.baseOffset && totalOffset< offset.baseOffset+offset.endOffset){
				return i;
			}
		} 
		return -1;
	}  
	
	public File getIndexDir() {
		return indexDir;
	}
	
	public int getBlockCount() {
		return blockCount;
	}     
	
	private File blockFile(long baseOffset){
		String fileName = String.format("%020d%s", baseOffset, BlockSuffix);
		File blockDir = new File(indexDir, BlockDir);
		return new File(blockDir, fileName);
	}

	private void writeOffset(int blockNumber, Offset offset) {
		buffer.position(IndexHeadSize + blockNumber * OffsetSize);
		
		buffer.putLong(offset.createdTime);
		buffer.putLong(offset.baseOffset);
		buffer.putInt(offset.endOffset); 
	}    
	
	private Block createBlock() throws IOException{
		if (blockCount >= BlockMaxCount) {
			throw new IllegalStateException("Offset table full");
		}
		long baseOffset = 0;
		if(blockCount > 0){
			Offset offset = getOffset(blockCount-1);
			baseOffset = offset.baseOffset + offset.endOffset;
		}
		
		Offset offset = new Offset(); 
		offset.createdTime = System.currentTimeMillis();
		offset.baseOffset = baseOffset;
		offset.endOffset = 0;
		
		writeOffset(blockCount, offset);
		
		blockCount++;
		writeBlockCount(); 
		
		Block block = new Block(this, blockFile(offset.baseOffset), blockCount-1);
		return block;
	} 
 
	private boolean isLastBlockFull(){
		if(blockCount < 1) return false;
		
		buffer.position(IndexHeadSize + (blockCount-1)*OffsetSize + 16);
		int endOffset = buffer.getInt();
		return endOffset >= BlockMaxSize;
	} 
	 
	private void writeBlockCount(){
		buffer.position(0); 
		buffer.putInt(blockCount);
	}  
	
	@Override
	protected void loadDefaultData() throws IOException { 
		buffer.position(0);
		this.blockCount = buffer.getInt(); 
	}

	@Override
	protected void writeDefaultData() throws IOException {
		writeBlockCount(); 
	} 
	
	public static class Offset {
		public long baseOffset;
		public long createdTime;
		public int endOffset; 
	}
}
