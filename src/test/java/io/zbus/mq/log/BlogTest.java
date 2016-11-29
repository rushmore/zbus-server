package io.zbus.mq.log;

import java.io.File;

public class BlogTest {
	
	static void test(String name) throws Exception { 
		File file = new File(name);
		final int dataSize = 10240;
		final int count = 6400;

		Block block = new Block(file);
		byte[] data = new byte[dataSize];
		int writeCount = 0;
		long start = System.currentTimeMillis();
		while (writeCount++ < count) {
			if (block.isFull())
				break;
			block.offer(data);
		}
		long end = System.currentTimeMillis();
		block.close();

		System.out.format("%.2f M/s\n", 1.0 * writeCount * dataSize * 1000 / (end - start) / 1024 / 1024);
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 10; i++)
			test("/tmp/test.zbus" + i);
	}
}
