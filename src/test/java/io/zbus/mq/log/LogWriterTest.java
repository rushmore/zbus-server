package io.zbus.mq.log;

import java.io.File;

public class LogWriterTest {
	
	public static void main(String[] args) throws Exception {
		LogWriter q = new LogWriter(new File("/tmp"), "MyMQ");
		for(int i=0;i<10000;i++){
			q.write(new byte[1024*1024]);
		}
	}
}
