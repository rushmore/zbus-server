package io.zbus.mq.disk;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
 

public class MappedFile implements Closeable {   
	private static final Logger log = LoggerFactory.getLogger(MappedFile.class); 
	
	public static final int HeadSize = 1024; 
	
	protected static final int CreatorPos = 512-128; //creator max length 127(another 1 byte for length)
	protected static final int CreatedTimePos = CreatorPos-8;
	protected static final int UpdatedTimePos = CreatorPos-16;
	protected static final int MaskPos = CreatorPos-20;
	
	protected volatile int mask;
	protected volatile long createdTime = System.currentTimeMillis();
	protected volatile long updatedTime = System.currentTimeMillis(); 
	protected volatile String creator; //max length 127
	
	// Extension
	protected static final int ExtItemSize = 128;
	protected static final int ExtItemCount = 4;
	protected static final int ExtOffset = HeadSize - ExtItemSize * ExtItemCount; 
	protected String[] extentions = new String[ExtItemCount];
	
	protected MappedByteBuffer buffer;  
	protected FileChannel fileChannel; 
	protected Lock lock = new ReentrantLock();
	
	private RandomAccessFile randomAccessFile; 
	private File diskFile;
	
	
	protected void load(File file, int fileSize) throws IOException {
		if(fileSize < 1024){
			throw new IllegalArgumentException("fileSize should >= 1024");
		}
		try { 
			boolean fileExits = file.exists();
			if(!fileExits){
				File parent = file.getParentFile();
				if(parent != null && !parent.exists()){ 
					parent.mkdirs();
				}  
			}  
			this.diskFile = file;
			randomAccessFile = new RandomAccessFile(diskFile, "rw");
			fileChannel = randomAccessFile.getChannel();
			buffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileSize).load(); 
			
			if (fileExits) { 
				long size = randomAccessFile.length();
				if (size < fileSize) {
					randomAccessFile.setLength(fileSize);
					randomAccessFile.seek(size);
					randomAccessFile.write(new byte[(int) (fileSize - size)]);
				} 
				
				buffer.position(CreatorPos); 
				byte len = buffer.get();
				if(len <= 0){
					creator = null;
				} else {
					byte[] data = new byte[127];
					buffer.get(data);
					creator = new String(data, 0, len);
				}
				buffer.position(CreatedTimePos); 
				createdTime = buffer.getLong();
				buffer.position(UpdatedTimePos);
				updatedTime = buffer.getLong();
				buffer.position(MaskPos);
				mask = buffer.getInt();
				
				readExt();
				
				loadDefaultData();
			} else { 
				
				buffer.position(CreatorPos); 
				buffer.put((byte)0); 
				
				buffer.position(CreatedTimePos); 
				buffer.putLong(createdTime);
				buffer.position(UpdatedTimePos);
				buffer.putLong(updatedTime);
				buffer.position(MaskPos);
				buffer.putInt(mask);
				
				initExt();
				
				writeDefaultData(); 
			}
			 
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new IllegalArgumentException(e);
		} 
	}  
	
	protected void loadDefaultData() throws IOException{
		
	}
	
	protected void writeDefaultData() throws IOException{
		
	} 
	
	public void delete() throws IOException{
		close(); 
		diskFile.delete();
	} 
	
	@Override
	public void close() throws IOException { 
		if(buffer == null) return;
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
			randomAccessFile.close();
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}  

	public int getMask() {
		return mask;
	}

	public void setMask(int value) {
		mask = value;
		try {
			lock.lock();
			buffer.position(MaskPos);
			buffer.putInt(mask);
		} finally {
			lock.unlock();
		}
	} 
	
	public long getCreatedTime() {
		return createdTime;
	}
	
	public long getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(long value) {
		updatedTime = value;
		try {
			lock.lock();
			buffer.position(UpdatedTimePos);
			buffer.putLong(updatedTime);
		} finally {
			lock.unlock();
		}
	} 
	
	public String getCreator() {
		return creator;
	}

	public void setCreator(String value) {
		if(value != null && value.length() > 127){
			throw new IllegalArgumentException("Creator("+value+") length should < 127");
		}
		this.creator = value;
		try {
			lock.lock();
			buffer.position(CreatorPos); 
			if(creator == null){
				this.buffer.put((byte)0);
			} else {
				buffer.put((byte)creator.length());
				buffer.put(creator.getBytes());
			}
		} finally {
			lock.unlock();
		}
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
}
