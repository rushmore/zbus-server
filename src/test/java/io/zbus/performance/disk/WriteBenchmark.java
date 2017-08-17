package io.zbus.performance.disk;

import java.io.File;
import java.io.RandomAccessFile;

public class WriteBenchmark {
	
	public static void main(String[] args) throws Exception {
		File file = new File("/tmp/bigdata");
		RandomAccessFile diskFile = new RandomAccessFile(file, "rw");
		long start = System.currentTimeMillis();
		
		int size = 1024*1024;
		int checkSize = 1000;
		for(int i=0;i<200000;i++){
			diskFile.write(new byte[size]);
			
			if((i+1)%checkSize == 0){
				long end = System.currentTimeMillis();
				System.out.format("%.2f M/s\n", checkSize*size*1000.0/1024/1024/(end-start));
				start = System.currentTimeMillis();
			}
		}
		diskFile.close(); 
	} 
}
