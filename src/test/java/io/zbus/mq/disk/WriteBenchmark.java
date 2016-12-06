package io.zbus.mq.disk;

import java.io.File;
import java.io.RandomAccessFile;

public class WriteBenchmark {
	
	public static void main(String[] args) throws Exception {
		File file = new File("/tmp/bigdata");
		RandomAccessFile diskFile = new RandomAccessFile(file, "rw");
		long start = System.currentTimeMillis();
		
		for(int i=0;i<200000;i++){
			diskFile.write(new byte[1024*1024]);
			
			if((i+1)%1000 == 0){
				long end = System.currentTimeMillis();
				System.out.format("%.2f M/s\n", 1000*1000.0/(end-start));
				start = System.currentTimeMillis();
			}
		}
		diskFile.close(); 
	} 
}
