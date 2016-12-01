package io.zbus.mq.log;

import java.io.File;

public class LogWriterTest {
	
	public static void main(String[] args) throws Exception {
		Index index = new Index(new File("/tmp/MyMQ"));
		
		LogWriter q = new LogWriter(index);
		for(int i=0;i<2000;i++){
			q.write(new byte[1024*1024]);
		}
		
		index.close();
	}
}
