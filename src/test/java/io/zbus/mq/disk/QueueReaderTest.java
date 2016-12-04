package io.zbus.mq.disk;

import java.io.File;

import io.zbus.mq.disk.Index;
import io.zbus.mq.disk.QueueReader;

public class QueueReaderTest {
	
	public static void main(String[] args) throws Exception {
		Index index = new Index(new File("C:/tmp/MyMQ"));  
		
		QueueReader reader = new QueueReader(index, "ConsumeGroup4"); 
		long count = 0;
		while(true){
			byte[] data = reader.read();
			if(data == null) break;
			count++; 
			if(count%1000 == 0){
				
				System.out.println(data.length+ ": " + count);
			}
		} 
		System.out.println(count);
		
		reader.close();
	} 
}
