package io.zbus.unittests.mq.disk;

import java.io.File;
import java.util.Iterator;

import io.zbus.mq.disk.Index;
import io.zbus.mq.disk.QueueNak;
import io.zbus.mq.disk.QueueNak.NakRecord;
import io.zbus.mq.disk.QueueReader;

public class QueueNakTest {
	public static void main(String[] args) throws Exception { 
		Index index = new Index(new File("/tmp/MyTopic"));
		QueueReader reader = new QueueReader(index, "MyTopic");
		QueueNak q = new QueueNak(reader);  
		//q.addNak(103, "xx");
		q.pollTimeoutMessage();
		Iterator<NakRecord> iter = q.iterator();
		while(iter.hasNext()) {
			NakRecord nak = iter.next();
			System.out.println(nak.offset);
		} 
		
		
		q.close(); 
		reader.close();
		index.close();
	} 
}
