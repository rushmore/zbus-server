package io.zbus.mq.diskq;

import java.io.File;
import java.io.IOException;

public class DiskQueue {
	
	private Index index;
	private Block writeBlock;

	public DiskQueue(File basePath, String queueName) throws IOException {
		if (!basePath.exists()) {
			basePath.mkdirs();
		}
		String indexFileName = queueName + Index.INDEX_SUFFIX;
		index = new Index(basePath, indexFileName); 
		writeBlock = index.buildWriteBlock();
	}
	
	public void write(byte[] data) throws IOException{
		if(writeBlock.isFull()){
			writeBlock.close();
			writeBlock = index.buildWriteBlock();
		}
		writeBlock.write(data);
		index.updateWriteOffset(writeBlock.getWriteOffset());
		
	}

	public static void main(String[] args) throws Exception {
		DiskQueue q = new DiskQueue(new File("/tmp"), "MyMQ");
		while(true){ 
			q.write(new byte[102400]);
		}
	}
}
