package io.zbus.mq.disk;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 
 * 000-- IndexVersion: 4 
 * 004-- BlockCount: 4
 * 008-- BlockStart: 8
 * 016-- Flag: 4
 * 020-- MessageCount: 8 
 * 
 * 
 * @author Rushmore
 *
 */
public class Index extends MappedFile {
	public static final int IndexVersion  = 0x01;
	public static final String IndexSuffix = ".idx";
	public static final String ReaderSuffix = ".rdx";
	public static final String BlockSuffix = ".zbus";
	public static final String BlockDir = "data";
	public static final String ReaderDir = "reader"; 
	public static final int BlockMaxCount = 10240;
	public static final long BlockMaxSize = 64 * 1024 * 1024; // default to 64M
	

	private static final int OffsetSize = 28;
	private static final int IndexHeadSize = 1024;
	private static final int IndexSize = IndexHeadSize + BlockMaxCount * OffsetSize;

	private static final int BlockCountPos = 4; 
	private static final int FlagPos = 16;
	private static final int MessageCountPos = 20;
	
	// Extension
	private static final int ExtItemSize = 128;
	private static final int ExtItemCount = 4;
	private static final int ExtOffset = IndexHeadSize - ExtItemSize * ExtItemCount; 
	
	private volatile int blockCount = 0;
	private volatile long blockStart = 0;
	private volatile int flag = 0;
	private volatile AtomicLong messageCount = new AtomicLong(0);
	
	public final AtomicReference<CountDownLatch> newDataAvailable = new AtomicReference<CountDownLatch>(new CountDownLatch(1));;

	private File indexDir;
	private Lock lock = new ReentrantLock();
	private final String name;

	private String[] extentions = new String[ExtItemCount];

	public Index(File dir) throws IOException {
		this.indexDir = dir;
		this.name = indexDir.getName();
		File file = new File(indexDir, this.indexDir.getName() + IndexSuffix);
		load(file, IndexSize);
	}  
	/**
	 * Write endOffset of last block
	 * 
	 * @param endOffset
	 *            writable offset of last block
	 * @throws IOException
	 */
	public void writeEndOffset(int endOffset) throws IOException {
		try {
			lock.lock();
			buffer.position(currentBlockPosition() + 16);
			buffer.putInt(endOffset);
			buffer.putLong(System.currentTimeMillis());
		} finally {
			lock.unlock();
		}
	} 

	public Block createWriteBlock() throws IOException {
		Offset offset = null; 
		try {
			lock.lock();
			if (blockCount < 1 || isCurrentBlockFull()) { 
				offset = addNewOffset();
			} else { 
				offset = readOffsetUnsafe(currentBlockNumber());
			}
		} finally {
			lock.unlock();
		} 
		Block block = new Block(this, blockFile(offset.baseOffset), currentBlockNumber());
		return block;
	}

	public Block createReadBlock(long blockNumber) throws IOException {
		if (blockCount < 1) {
			throw new IllegalStateException("No block to read");
		}
		checkBlockNumber(blockNumber);

		Offset offset = readOffset(blockNumber);
		Block block = new Block(this, blockFile(offset.baseOffset), blockNumber);
		return block;
	}

	public Offset readOffset(long blockNumber) throws IOException {
		checkBlockNumber(blockNumber);
		try {
			lock.lock();
			return readOffsetUnsafe(blockNumber);
		} finally {
			lock.unlock();
		}
	}

	private Offset readOffsetUnsafe(long blockNumber) throws IOException {
		buffer.position(blockPosition(blockNumber));

		Offset offset = new Offset();
		offset.createdTime = buffer.getLong();
		offset.baseOffset = buffer.getLong();
		offset.endOffset = buffer.getInt();
		offset.updatedTime = buffer.getLong();
		return offset;
	}

	public void checkBlockNumber(long blockNumber) {
		if (blockNumber-blockStart >= blockCount) {
			throw new IllegalArgumentException(
					"blockNumber should >="+blockStart +" and <" + (blockStart+blockCount) + ", but was " + blockNumber);
		}
	}

	/**
	 * Search block number by totalOffset
	 * 
	 * @param readOffset
	 *            offset from block 0, not the offset in the block.
	 * @return block number the tottalOffset follows.
	 * @throws IOException
	 */
	public int searchBlockNumber(long totalOffset) throws IOException {
		for (int i = 0; i < blockCount; i++) {
			long blockNumber = blockStart + i;
			Offset offset = readOffset(blockNumber);
			if (totalOffset >= offset.baseOffset && totalOffset < offset.baseOffset + offset.endOffset) {
				return i;
			}
		}
		return -1;
	}
	
	public long increaseMessageCount(){
		try {
			lock.lock();
			buffer.position(MessageCountPos); 
			long count = messageCount.incrementAndGet();
			buffer.putLong(count);
			return count;
		} finally {
			lock.unlock();
		} 
	}  

	public File getIndexDir() {
		return indexDir;
	}
	
	public File getReaderDir(){
		return new File(indexDir,ReaderDir);
	}
	
	public static boolean isReaderFile(File file){
		return file.getName().endsWith(ReaderSuffix);
	} 

	public int getBlockCount() {
		return blockCount;
	}
	public long getBlockStart(){
		return this.blockStart;
	}
	
	public long currentBlockNumber(){ 
		return blockStart+blockCount-1;
	} 
	
	private int currentBlockPosition(){ 
		return blockPosition(currentBlockNumber());
	}
	
	private int blockPosition(long blockNumber){ 
		return (int)(IndexHeadSize + (blockNumber%BlockMaxCount)*OffsetSize);
	}
	
	public int currentWriteOffset() throws IOException{
		return readOffset(currentBlockNumber()).endOffset;
	}
	
	public boolean overflow(long blockNumber){
		return blockNumber>=(blockStart+blockCount);
	}
	
	public long shrinkBySize(long targetSize) throws IOException{
		if(targetSize <=0){
			throw new IllegalArgumentException("targetSize should > 0, but = " + targetSize);
		}
		try {
			lock.lock();
			int remainBlockCount = (int)(targetSize/BlockMaxSize);
			if(remainBlockCount <= 0){
				return Long.MAX_VALUE; //all deleted
			}
			if(remainBlockCount >= blockCount){
				remainBlockCount = blockCount;
			}
			Offset offset = readOffsetUnsafe(blockStart+blockCount-remainBlockCount);
			blockCount = remainBlockCount;
			blockStart += (blockCount-remainBlockCount);
			
			return offset.baseOffset + offset.endOffset;
		} finally {
			lock.unlock();
		}  
	} 
	
	public long shrinkByTime(long startTime) throws IOException{ 
		try {
			lock.lock();
			if(startTime >= System.currentTimeMillis()){
				return Long.MAX_VALUE;
			}
			int remainBlockCount = blockCount;
			for(long i=blockStart;i<blockStart+blockCount;i++){
				Offset offset = readOffsetUnsafe(i);
				if(startTime < offset.updatedTime) break;
				remainBlockCount--;
			}
			
			Offset offset = readOffsetUnsafe(blockStart+blockCount-remainBlockCount);
			blockCount = remainBlockCount;
			blockStart += (blockCount-remainBlockCount);
			
			return offset.baseOffset + offset.endOffset;
		} finally {
			lock.unlock();
		}  
	} 
	
	private File blockFile(long baseOffset) {
		String fileName = String.format("%020d%s", baseOffset, BlockSuffix);
		File blockDir = new File(indexDir, BlockDir);
		return new File(blockDir, fileName);
	}

	private void writeOffset(long blockNumber, Offset offset) {
		buffer.position(blockPosition(blockNumber));

		buffer.putLong(offset.createdTime);
		buffer.putLong(offset.baseOffset);
		buffer.putInt(offset.endOffset);
		buffer.putLong(offset.updatedTime);
	} 
	
	private Offset addNewOffset() throws IOException {
		if (blockCount >= BlockMaxCount) {
			throw new IllegalStateException("Offset table full");
		}
 
		long baseOffset = 0;
		if (blockCount > 0) {
			Offset offset = readOffsetUnsafe(currentBlockNumber());
			baseOffset = offset.baseOffset + offset.endOffset;
		}

		Offset offset = new Offset();
		offset.updatedTime = offset.createdTime = System.currentTimeMillis();
		offset.baseOffset = baseOffset;
		offset.endOffset = 0; 
		
		blockCount++;
		writeBlockCount();
		
		writeOffset(currentBlockNumber(), offset); 

		return offset;
	}

	private boolean isCurrentBlockFull() {
		if (blockCount < 1)
			return false;

		buffer.position(currentBlockPosition() + 16);
		int endOffset = buffer.getInt();
		return endOffset >= BlockMaxSize;
	}

	private void writeBlockCount() {
		buffer.position(BlockCountPos);
		buffer.putInt(blockCount);
	}

	@Override
	protected void loadDefaultData() throws IOException {
		buffer.position(0);
		int version = buffer.getInt();
		if(version != IndexVersion){
			throw new IllegalStateException("IndexVersion NOT matched");
		} 
		this.blockCount = buffer.getInt(); 
		this.blockStart = buffer.getLong(); 
		this.flag = buffer.getInt();
		this.messageCount.set(buffer.getLong());
		
		readExt();
	}

	@Override
	protected void writeDefaultData() throws IOException {
		buffer.position(0);
		buffer.putInt(IndexVersion);
		buffer.putInt(blockCount);
		buffer.putLong(this.blockStart);
		buffer.putInt(this.flag);
		buffer.putLong(this.messageCount.get());
		initExt();
	}

	private void initExt() {
		for (int i = 0; i < ExtItemCount; i++) {
			setExt(i, null);
		}
	}

	private void readExt() throws IOException {
		for (int i = 0; i < ExtItemCount; i++) {
			readExtByIndex(i);
		}
	}

	private void readExtByIndex(int idx) throws IOException {
		this.buffer.position(ExtOffset + ExtItemSize * idx);
		int len = buffer.get();
		if (len <= 0) {
			this.extentions[idx] = null;
			return;
		}
		if (len > ExtItemSize - 1) {
			throw new IOException("length of extension field invalid, too long");
		}
		byte[] bb = new byte[len];
		this.buffer.get(bb);
		this.extentions[idx] = new String(bb);
	}

	public void setExt(int idx, String value) {
		if (idx < 0) {
			throw new IllegalArgumentException("idx must >=0");
		}
		if (idx >= ExtItemCount) {
			throw new IllegalArgumentException("idx must <" + ExtItemCount);
		}
		try {
			lock.lock();
			this.extentions[idx] = value;
			this.buffer.position(ExtOffset + ExtItemSize * idx);
			if (value == null) {
				this.buffer.put((byte) 0);
				return;
			}
			if (value.length() > ExtItemSize - 1) {
				throw new IllegalArgumentException(value + " too long");
			}
			this.buffer.put((byte) value.length());
			this.buffer.put(value.getBytes());
		} finally {
			lock.unlock();
		}
	}

	public String getExt(int idx) {
		if (idx < 0) {
			throw new IllegalArgumentException("idx must >=0");
		}
		if (idx >= ExtItemCount) {
			throw new IllegalArgumentException("idx must <" + ExtItemCount);
		}
		return this.extentions[idx];
	}

	public int getFlag() {
		return flag;
	}

	public void setFlag(int flag) {
		this.flag = flag;
		try {
			lock.lock();
			this.buffer.position(FlagPos);
			this.buffer.putInt(this.flag);
		} finally {
			lock.unlock();
		}
	} 
	
	public long getMessageCount(){
		return this.messageCount.get();
	}

	public String getName() {
		return name;
	} 

	public static class Offset {
		public long baseOffset;
		public long createdTime;
		public int endOffset;
		public long updatedTime; 
	}
}
