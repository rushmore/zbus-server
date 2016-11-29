package io.zbus.mq.diskq;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;

import io.zbus.util.log.Logger;
import io.zbus.util.log.LoggerFactory;

/**
 * --[4] OffsetCount 
 * --[~1024] Extension 
 * --[24 bytes] -- Offset
 * 
 * 
 * @author Rushmore
 *
 */

public class Index implements Closeable {
	public static final String INDEX_SUFFIX = ".idx";
	private static final Logger log = LoggerFactory.getLogger(Index.class);
	
	public static final int OFFSET_SIZE = 20;
	public static final int BLOCK_MAX_COUNT = 10240; 
	public static final int INDEX_HEAD_SIZE = 1024; 
	// Capacity: 10K Offset.
	public static final int INDEX_SIZE = INDEX_HEAD_SIZE + BLOCK_MAX_COUNT * OFFSET_SIZE; 
 
	private volatile int blockCount = 0;
	private volatile int writeOffset = 0;

	private RandomAccessFile indexFile;
	private FileChannel fileChannel;
	private MappedByteBuffer buffer;
	private File directory;

	public Index(File dir, String indexFileName) {
		if (indexFileName == null) {
			throw new IllegalArgumentException("indexFileName null");
		}
		if (indexFileName.length() > 127) {
			throw new IllegalArgumentException("indexFileName: " + indexFileName + " longer than 127");
		}
		this.directory = dir;
 
		File file = new File(directory, indexFileName);
		try {
			if (file.exists()) {
				this.indexFile = new RandomAccessFile(file, "rw");
				long size = indexFile.length();
				if (size < INDEX_SIZE) {
					indexFile.setLength(INDEX_SIZE);
					indexFile.seek(size);
					indexFile.write(new byte[(int) (INDEX_SIZE - size)]);
				}
				fileChannel = indexFile.getChannel();
				buffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, INDEX_SIZE);
				buffer = buffer.load();
				buffer.position(0);
				this.blockCount = buffer.getInt();
				this.writeOffset = buffer.getInt();
			} else {
				File parent = file.getParentFile();
				if(parent != null){ 
					parent.mkdirs();
				} 
				
				indexFile = new RandomAccessFile(file, "rw");
				fileChannel = indexFile.getChannel();
				buffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, INDEX_SIZE);
				putBlockCount();
				putWriteOffset();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new IllegalArgumentException(e);
		}
	} 

	public int getBlockCount() {
		return blockCount;
	}  
	
	public void updateWriteOffset(int offset){
		buffer.position(4); 
		buffer.putInt(offset);
		this.writeOffset = offset;
	}
	 
	public Block buildWriteBlock() throws IOException{
		if(blockCount < 1){
			return createBlock();
		}
		
		Offset offset = getOffset(blockCount-1);
		Block block = new Block(new File(directory, blockName(offset.baseOffset)));
		if(block.isFull()){ 
			offset.endOffset = block.getWriteOffset(); 
			writeOffset(blockCount-1, offset);
			
			block.close(); 
			return createBlock();
		}
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
		writeOffset = 0;
		putWriteOffset(); 
		
		Block block = new Block(new File(directory, blockName(offset.baseOffset)));
		return block;
	}
	
	private String blockName(long baseOffset){
		String blockFileName = String.format("%020d%s", baseOffset, Block.BLOCK_FILE_SUFFIX);
		return blockFileName;
	}
 
	 
	private void putBlockCount(){
		buffer.position(0); 
		buffer.putInt(blockCount);
	}
	
	private void putWriteOffset(){
		buffer.position(4); 
		buffer.putInt(writeOffset);
	}

	public Offset getOffset(int idx) {
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
	 

	private void writeOffset(int idx, Offset offset) {
		buffer.position(INDEX_HEAD_SIZE + idx * OFFSET_SIZE);
		
		buffer.putLong(offset.createdTime);
		buffer.putLong(offset.baseOffset);
		buffer.putInt(offset.endOffset); 
	}

	@Override
	public void close() throws IOException {
		try {
			AccessController.doPrivileged(new PrivilegedAction<Object>() { 
				public Object run() {
					try {
						Method getCleanerMethod = buffer.getClass().getMethod("cleaner");
						getCleanerMethod.setAccessible(true);
						
						Object cleaner = getCleanerMethod.invoke(buffer); 
						Method cleanMethod = cleaner.getClass().getMethod("clean");
						cleanMethod.setAccessible(true);
						cleanMethod.invoke(cleaner); 
					} catch (Exception e) { 
						log.error(e.getMessage(), e);
					}
					return null;
				}
			}); 
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {	
			fileChannel.close();
			indexFile.close();
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	
	public static class Offset {
		public long baseOffset;
		public long createdTime;
		public int endOffset; 
	}
}
