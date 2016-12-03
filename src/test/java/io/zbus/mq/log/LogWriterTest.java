package io.zbus.mq.log;

import java.io.File;

public class LogWriterTest {
	
	public static void main(String[] args) throws Exception {
		
		Index index = new Index(new File("/tmp/MyMQ"));
		
		LogWriter q = new LogWriter(index);
		
		long start = System.currentTimeMillis();
		for(int i=0;i<200000;i++){
			q.write(new byte[1024*1024]);
			
			if((i+1)%1000 == 0){
				long end = System.currentTimeMillis();
				System.out.format("%.2f M/s\n", 1000*1000.0/(end-start));
				start = System.currentTimeMillis();
			}
		}
		
		index.close();
	}
}
