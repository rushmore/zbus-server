package io.zbus.performance.disk;

import java.io.File;

import io.zbus.mq.disk.Index;
import io.zbus.mq.disk.QueueReader;
import io.zbus.performance.Perf;

public class QueueReaderPerf {
	
	public static void main(String[] args) throws Exception { 
		Index index = new Index(new File("C:/tmp/MyMQ"));  
		final QueueReader qr = new QueueReader(index, "Reader1");
		
		
		Perf perf = new Perf(){ 
			
			@Override
			public TaskInThread buildTaskInThread() {
				return new TaskInThread(){  
					@Override
					public void initTask() throws Exception { 
						
					}
					
					@Override
					public void doTask() throws Exception { 
						qr.read();
					}
				};
			}  
		}; 
		
		perf.loopCount = 20000000;
		perf.threadCount = 1;
		perf.logInterval = 10000;
		perf.run();
		
		perf.close(); 
		
		qr.close();
		index.close();
	} 
}
