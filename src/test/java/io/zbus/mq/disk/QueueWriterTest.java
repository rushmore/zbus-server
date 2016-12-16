package io.zbus.mq.disk;

import java.io.File;

import io.zbus.mq.diskq.Index;
import io.zbus.mq.diskq.QueueReader;
import io.zbus.mq.diskq.QueueWriter;

public class QueueWriterTest {
	
	public static void main(String[] args) throws Exception { 
		Index index = new Index(new File("C:/tmp/StringQueue")); 
		QueueWriter w = new QueueWriter(index);
		
		for(int i=0; i<0;i++){
			w.write(new String("hello"+i).getBytes());
		}
		
		QueueReader r = new QueueReader(index, "MyGroup4");
		while(true){
			byte[] data = r.read();
			if(data == null) break;
			System.out.println(new String(data));
		}
		
		r.close();
		index.close();
	}
	
}
