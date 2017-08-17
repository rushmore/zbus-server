package io.zbus.performance.disk;

import java.io.File;
import java.io.RandomAccessFile;

public class ReadBenchmark {
	//
	public static void main(String[] args) throws Exception {
		File file = new File("/tmp/bigdata"); 
		RandomAccessFile rfile = new RandomAccessFile(file, "r");
		long start = System.currentTimeMillis();
		int size = 300;
		int checkSize = 10000; 
		
		long pos = 0;
		for(int i=0;i<20000000;i++){   
			rfile.seek(pos); 
				pos += size;
				int n = rfile.read(new byte[size]); 
				if(n != size){
					rfile.close();
					System.out.println("EOF");
					return;
				} 
			
			if((i+1)%checkSize == 0){ 
				long end = System.currentTimeMillis();
				System.out.format("%.2f M/s\n", checkSize*1000.0*size/1024/1024/(end-start));
				System.out.format("%.2f r/s\n", checkSize*1000.0/(end-start));
				start = System.currentTimeMillis();
			}
		}  
		rfile.close();
	} 
}
