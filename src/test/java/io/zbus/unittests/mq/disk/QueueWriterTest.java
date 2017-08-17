package io.zbus.unittests.mq.disk;

import java.io.File;

import io.zbus.mq.disk.DiskMessage;
import io.zbus.mq.disk.Index;
import io.zbus.mq.disk.QueueReader;
import io.zbus.mq.disk.QueueWriter;

public class QueueWriterTest {
	
	public static void main(String[] args) throws Exception { 
		Index index = new Index(new File("C:/tmp/MyMQ")); 
		QueueWriter w = new QueueWriter(index);
		
		for(int i=0; i<1000000;i++){
			DiskMessage message = new DiskMessage();
			message.body = new String("hello"+i).getBytes();
			w.write(message);
		}
		
		QueueReader r = new QueueReader(index, "MyMQ");
		while(true){
			DiskMessage data = r.read();
			if(data == null) break;
			System.out.println(new String(data.body));
		}
		
		r.close();
		w.close();
		index.close(); 
	}
	
}
