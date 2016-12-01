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
	public static final String INDEX_SUFFIX    = ".idx";  
	public static final String BLOCK_DIR_NAME  = "data"; 
	public static final String READER_DIR_NAME = "reader";
	public static final String BLOCK_FILE_SUFFIX = ".zbus"; 
	public static final int OFFSET_SIZE = 20;
	public static final int BLOCK_MAX_COUNT = 10240; 
	public static final int INDEX_HEAD_SIZE = 1024;  
	public static final int INDEX_SIZE = INDEX_HEAD_SIZE + BLOCK_MAX_COUNT * OFFSET_SIZE;  
	public static final long MAX_BLOCK_SIZE = 64*1024*1024; //default to 64M
	 
	private volatile int blockCount = 0;  
	public final AtomicReference<CountDownLatch> newDataAvailable = new AtomicReference<CountDownLatch>(new CountDownLatch(1));; 
	  
	private File indexDir; 
	private File blockDir;
	
	public Index(File dir) {
		String indexFileName = dir.getName(); 
		if (indexFileName.length() > 127) {
			throw new IllegalArgumentException("IndexDirName: " + dir + " longer than 127");
		}
		
		this.indexDir = dir;
		this.blockDir = new File(this.indexDir, BLOCK_DIR_NAME);
 
		File file = new File(indexDir, indexFileName + INDEX_SUFFIX); 
		load(file, INDEX_SIZE); 
	} 
	
	@Override
	protected void loadDefaultData() throws IOException { 
		buffer.position(0);
		this.blockCount = buffer.getInt(); 
	}

	@Override
	protected void writeDefaultData() throws IOException {
		putBlockCount(); 
	}
	
	public File getIndexDir() {
		return indexDir;
	}
	
	public int getBlockCount() {
		return blockCount;
	}  
	 
	public Offset getOffset(int idx) {
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
	
	public void updateWriteOffset(int offset){  
		buffer.position(INDEX_HEAD_SIZE + (blockCount-1)*OFFSET_SIZE + 16);
		buffer.putInt(offset);
	}
	
	public int getWriteOffset(){
		buffer.position(INDEX_HEAD_SIZE + (blockCount-1)*OFFSET_SIZE + 16);
		return buffer.getInt();
	}

	private void writeOffset(int idx, Offset offset) {
		buffer.position(INDEX_HEAD_SIZE + idx * OFFSET_SIZE);
		
		buffer.putLong(offset.createdTime);
		buffer.putLong(offset.baseOffset);
		buffer.putInt(offset.endOffset); 
	}   
	
	public Block buildWriteBlock() throws IOException{
		if(blockCount < 1 || isCurrentBlockFull()){
			return createBlock();
		}
		
		Offset offset = getOffset(blockCount-1);
		Block block = new Block(this, new File(blockDir, blockName(offset.baseOffset))); 
		return block;
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
		putBlockCount(); 
		
		Block block = new Block(this, new File(blockDir, blockName(offset.baseOffset)));
		return block;
	}
	
	private String blockName(long baseOffset){
		return String.format("%020d%s", baseOffset, BLOCK_FILE_SUFFIX);
	}
 
	private boolean isCurrentBlockFull(){
		if(blockCount < 1) return false;
		
		buffer.position(INDEX_HEAD_SIZE + (blockCount-1)*OFFSET_SIZE + 16);
		int endOffset = buffer.getInt();
		return endOffset >= MAX_BLOCK_SIZE;
	} 
	 
	private void putBlockCount(){
		buffer.position(0); 
		buffer.putInt(blockCount);
	} 

	
	public static class Offset {
		public long baseOffset;
		public long createdTime;
		public int endOffset; 
	}
}
