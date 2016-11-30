package io.zbus.mq.log;

import java.io.File;

public class LogReaderTest {
	
	public static void main(String[] args) throws Exception {
		Index index = new Index(new File("/tmp/MyMQ")); 
		for(int i = 0; i<10000;i++){
			LogReader reader = new LogReader(index, "ConsumeGroup"+i);
			reader.close();
		}
	} 
}
