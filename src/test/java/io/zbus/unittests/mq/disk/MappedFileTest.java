package io.zbus.unittests.mq.disk;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import io.zbus.mq.disk.MappedFile;
 

public class MappedFileTest {

	@Test
	public void testRemove() {
		File diskFile = new File("/tmp/toremove");
		try {
			MappedFile file = new MyMappedFile(diskFile); 
			Assert.assertTrue(diskFile.exists());
			
			file.delete(); 
			
			Assert.assertFalse(diskFile.exists());
			
			file.close();
			
		} catch (IOException e) {
			fail("Mapped file creation failed");
		} 
	}

}
