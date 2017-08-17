package io.zbus.unittests.mq.disk;

import java.io.File;
import java.io.IOException;

import io.zbus.mq.disk.MappedFile;

public class MyMappedFile extends MappedFile {
	private int testData = 0;

	public MyMappedFile(File file) throws IOException {
		load(file, 1024);
	}

	@Override
	protected void loadDefaultData() throws IOException {
		buffer.position(0);
		testData = buffer.getInt();
	}

	@Override
	protected void writeDefaultData() throws IOException {
		buffer.position(0);
		buffer.putInt(testData);

	}
	
	public int getData(){
		buffer.position(0);
		return buffer.getInt();
	}
	
	public void putData(int data){
		this.testData = data;
		buffer.position(0);
		buffer.putInt(this.testData);
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 2; i++) {
			Thread thread = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						MyMappedFile file = new MyMappedFile(new File("/tmp/testdata"));
						while (true) {
							System.out.println(file.getData());
							try {
								Thread.sleep(200);
							} catch (InterruptedException e) {
								break;
							}
						} 
						file.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			thread.start();
		}
		MyMappedFile file = new MyMappedFile(new File("/tmp/testdata"));
		int i = 0;
		while(true){
			file.putData(++i);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				break;
			}
		}
		file.close();
	}
}
