package io.zbus.mq.log;

import java.io.File;
import java.io.IOException;

public class LogReader extends MappedFile {
	public static final int READER_FILE_SIZE = 256;  
	private Block readBlock;  
	private final Index index;  
	private final String readerGroup;
	private long readOffset = 0; 
	
	public LogReader(Index index, String readerGroup){
		this.index = index; 
		this.readerGroup = readerGroup; 
		File readerDir = new File(index.getIndexDir(), Index.READER_DIR_NAME);
		File file = new File(readerDir, this.readerGroup);
		
		load(file, READER_FILE_SIZE); 
	}   
	
	
	@Override
	public void loadDefaultData() throws IOException {
		buffer.position(0);
		this.readOffset = buffer.getLong(); 
	}
	
	@Override
	public void writeDefaultData() throws IOException {
		this.readOffset = 0;
		putReadOffset(); 
	}
	
	private void putReadOffset(){
		buffer.position(0); 
		buffer.putLong(readOffset);
	}    
	
} 
