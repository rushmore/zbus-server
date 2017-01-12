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

import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory;
 

public class MappedFile implements Closeable {   
	private static final Logger log = LoggerFactory.getLogger(MappedFile.class); 
	
	protected MappedByteBuffer buffer;  
	protected FileChannel fileChannel;  
	
	private RandomAccessFile diskFile; 
	
	protected void load(File file, int fileSize) throws IOException {  
		try { 
			boolean fileExits = file.exists();
			if(!fileExits){
				File parent = file.getParentFile();
				if(parent != null && !parent.exists()){ 
					parent.mkdirs();
				}  
			}  
			
			diskFile = new RandomAccessFile(file, "rw");
			fileChannel = diskFile.getChannel();
			buffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileSize).load(); 
			
			if (fileExits) { 
				long size = diskFile.length();
				if (size < fileSize) {
					diskFile.setLength(fileSize);
					diskFile.seek(size);
					diskFile.write(new byte[(int) (fileSize - size)]);
				} 
				loadDefaultData();
			} else { 
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
			diskFile.close();
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	} 
}
