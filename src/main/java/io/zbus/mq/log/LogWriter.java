package io.zbus.mq.log;

import java.io.File;
import java.io.IOException;

public class LogWriter { 
	private Index index;
	private Block writeBlock;

	public LogWriter(File basePath, String queueName) throws IOException {
		File queueFile = new File(basePath, queueName);
		if (!queueFile.exists()) {
			queueFile.mkdirs();
		}
		String indexFileName = queueName + Index.INDEX_SUFFIX;
		index = new Index(queueFile, indexFileName); 
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
		LogWriter q = new LogWriter(new File("/tmp"), "MyMQ");
		for(int i=0;i<10000;i++){
			q.write(new byte[1024*1024]);
		}
	}
}
