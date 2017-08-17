package io.zbus.unittests.mq.disk;

import java.io.File;
import java.util.UUID;

import io.zbus.mq.disk.DiskMessage;
import io.zbus.mq.disk.Index;
import io.zbus.mq.disk.QueueReader;
import io.zbus.mq.disk.QueueWriter;

public class IndexTest {
	public static void main(String[] args) throws Exception { 
		Index index = new Index(new File("/tmp/TestQ"));
		QueueWriter writer = new QueueWriter(index);
		DiskMessage message = new DiskMessage();
		message.id = UUID.randomUUID().toString();
		message.tag = "tag";
		message.body = new byte[102];
		writer.write(message);
		
		QueueReader reader = new QueueReader(index, "MyGroup");
		message = reader.read();
		
		System.out.println(message);
		reader.close();
		writer.close();
	}
	
}
