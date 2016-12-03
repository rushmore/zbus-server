package io.zbus.mq.log;

import java.io.File;
import java.io.IOException;
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
	public static final String SUFFIX_INDEX = ".idx";  
	public static final String SUFFIX_BLOCK = ".zbus";  
	public static final String DIR_BLOCK    = "data"; 
	public static final String DIR_READER   = "reader"; 
	
	public static final int  BLOCK_MAX_COUNT = 10240; 
	public static final long BLOCK_MAX_SIZE  = 64*1024*1024; //default to 64M
	
	public static final int OFFSET_SIZE     = 20;
	public static final int INDEX_HEAD_SIZE = 1024;  
	public static final int INDEX_SIZE      = INDEX_HEAD_SIZE + BLOCK_MAX_COUNT * OFFSET_SIZE;  
	
	 
	private volatile int blockCount = 0;  
	public final AtomicReference<CountDownLatch> newDataAvailable = new AtomicReference<CountDownLatch>(new CountDownLatch(1));; 
	  
	private File indexDir;  
	
	public Index(File dir) {
		String indexFileName = dir.getName(); 
		if (indexFileName.length() > 127) {
			throw new IllegalArgumentException("IndexDirName: " + dir + " longer than 127");
		}
		
		this.indexDir = dir; 
 
		File file = new File(indexDir, indexFileName + SUFFIX_INDEX); 
		load(file, INDEX_SIZE); 
	}  
	
	public void writeOffset(int offset){  
		buffer.position(INDEX_HEAD_SIZE + (blockCount-1)*OFFSET_SIZE + 16);
		buffer.putInt(offset);
	}
	
	public int readOffset(){
		buffer.position(INDEX_HEAD_SIZE + (blockCount-1)*OFFSET_SIZE + 16);
		return buffer.getInt();
	}
	
	public Block createWriteBlock() throws IOException{
		if(blockCount < 1 || isCurrentBlockFull()){
			return createBlock();
		}
		
		Offset offset = getOffset(blockCount-1);
		Block block = new Block(this, blockFile(offset.baseOffset)); 
		return block;
	}
	
	public Block createReadBlock(int idx) throws IOException{
		if(blockCount < 1){
			throw new IllegalStateException("No block to read");
		}
		if(idx < 0 || idx >= blockCount){
			throw new IllegalArgumentException("Idx=" + idx + " should be>=0 and <"+blockCount);
		}
		
		Offset offset = getOffset(idx);
		Block block = new Block(this, blockFile(offset.baseOffset)); 
		return block;
	}
	
	public int findBockIndex(long readOffset) throws IOException{
		if(blockCount < 1){
			throw new IllegalStateException("No block to read");
		} 
		 
		for(int i=0; i<blockCount; i++){
			Offset offset = getOffset(i);
			if(readOffset >= offset.baseOffset && readOffset< offset.baseOffset+offset.endOffset){
				return i;
			}
		}
		throw new IllegalArgumentException("Offset=" + readOffset + " is not in range"); 
	} 
	
	
	public File getIndexDir() {
		return indexDir;
	}
	
	public int getBlockCount() {
		return blockCount;
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
	
	private File blockFile(long baseOffset){
		String fileName = String.format("%020d%s", baseOffset, SUFFIX_BLOCK);
		File blockDir = new File(indexDir, DIR_BLOCK);
		return new File(blockDir, fileName);
	}

	private void writeOffset(int idx, Offset offset) {
		buffer.position(INDEX_HEAD_SIZE + idx * OFFSET_SIZE);
		
		buffer.putLong(offset.createdTime);
		buffer.putLong(offset.baseOffset);
		buffer.putInt(offset.endOffset); 
	}    
	
	private Block createBlock() throws IOException{
		if (blockCount >= BLOCK_MAX_COUNT) {
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
		
		Block block = new Block(this, blockFile(offset.baseOffset));
		return block;
	}
	 
 
	private boolean isCurrentBlockFull(){
		if(blockCount < 1) return false;
		
		buffer.position(INDEX_HEAD_SIZE + (blockCount-1)*OFFSET_SIZE + 16);
		int endOffset = buffer.getInt();
		return endOffset >= BLOCK_MAX_SIZE;
	} 
	 
	private void writeBlockCount(){
		buffer.position(0); 
		buffer.putInt(blockCount);
	} 

	private Offset getOffset(int idx) {
		if(idx < 0){
			throw new IllegalArgumentException("idx = "+idx +", should >= 0");
		}
		
		if(idx >= BLOCK_MAX_COUNT){
			throw new IllegalArgumentException("idx = "+idx +", should not >="+BLOCK_MAX_COUNT);
		}
		
		buffer.position(INDEX_HEAD_SIZE + idx * OFFSET_SIZE);
		
		Offset offset = new Offset();
		offset.createdTime = buffer.getLong();
		offset.baseOffset = buffer.getLong(); 
		offset.endOffset = buffer.getInt(); 
		return offset;
	}
	
	private static class Offset {
		public long baseOffset;
		public long createdTime;
		public int endOffset; 
	}
}
