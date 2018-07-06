package io.zbus.mq.disk.support;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class BlockReadBuffer {
	private static final int BUFFER_SIZE = 1024*1024; //TODO, Make it configuarable
	private RandomAccessFile file;
	private byte[] buffer;
	private long pos = 0;  //the position of File where the buffer loaded 
	private int offset = 0;
	private int bufferLen = 0; 
	
	public BlockReadBuffer(RandomAccessFile file){
		this.file = file; 
		this.buffer = new byte[BUFFER_SIZE];
	}
	
	public synchronized int remaining(){
		return bufferLen - offset;
	}
	
	private void loadBuffer() throws IOException{
		this.file.seek(this.pos);
		this.offset = 0;
		this.bufferLen = this.file.read(buffer);
		if(bufferLen < 0){
			this.bufferLen = 0;
		}
	}
	 
	
	public void seek(long pos) throws IOException{  
		if(pos < this.pos || pos >= (this.pos + this.bufferLen)){
			//reset buffer
			this.pos = pos;
			loadBuffer();
			return;
		}  
		
		this.offset = (int)(pos - this.pos);
	}  
	
	public int skipBytes(int n) throws IOException { 
		if(n <= 0) return 0;
		this.offset += n;
		if(this.offset > bufferLen){ 
			this.pos += this.offset; 
			loadBuffer();
		}  
		
		return n;
	}
	
	public boolean checksum(int size, long checksum){  
		byte[] data = new byte[size]; 
		try {
			int n = peek(data);
			if(n <= 0){
				return false;
			}
		} catch (IOException e) {
			return false;
		} 
		
		Checksum crc = new CRC32();
		crc.update(data, 0, size);
		return checksum == crc.getValue();
	}
	
	public static long calcChecksum(byte[] data){
		Checksum crc = new CRC32();
		crc.update(data, 0, data.length);
		return crc.getValue();
	}
	
	public int read(byte[] data) throws IOException{   
		int required = data.length;
		if(required <= remaining()){
			System.arraycopy(this.buffer, offset, data, 0, required);
			offset += required;
			return required;
		}
		
		int dst = 0;
		while(required > 0){
			if(remaining() <= 0){
				this.pos += bufferLen;
				this.loadBuffer();
				if(bufferLen <= 0) return dst; //EOF
			}
			
			int bufRemaining = remaining();
			int n = required>=bufRemaining? bufRemaining : required;
			System.arraycopy(this.buffer, offset, data, dst, n);
			offset += n;
			dst += n;
			required -= n; 
		} 
		return dst;
	}
	
	public int peek(byte[] data) throws IOException{   
		int required = data.length;
		if(required <= remaining()){
			System.arraycopy(this.buffer, offset, data, 0, required); 
			return required;
		}
		 
		
		long peekPos = this.pos + offset; 
		this.file.seek(peekPos);
		int n = this.file.read(data);
		return n;
	}
	
	
	public int readInt() throws IOException{ 
		byte[] data = new byte[4];
		int n = read(data);
		if(n != 4){
			throw new IllegalStateException("Not enought data");
		} 
		int ch1 = data[0]&0XFF;
        int ch2 = data[1]&0XFF;
        int ch3 = data[2]&0XFF;
        int ch4 = data[3]&0XFF; 
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
	}
	
	
	public long readLong() throws IOException{ 
		return ((long)(readInt()) << 32) + (readInt() & 0xFFFFFFFFL);
	}
	 
}
