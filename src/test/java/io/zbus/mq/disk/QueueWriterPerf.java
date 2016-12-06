package io.zbus.mq.disk;

import java.io.File;

import io.zbus.mq.disk.Index;
import io.zbus.mq.disk.QueueWriter;

public class QueueWriterPerf {
	
	public static void main(String[] args) throws Exception { 
		Index index = new Index(new File("C:/tmp/MyMQ")); 
		QueueWriter q = new QueueWriter(index);
		
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
