package io.zbus.mq.disk;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
 
class Block implements Closeable {  
	private final Index index; 
	private final long blockNumber; 
	private final long baseOffset;
	
	private RandomAccessFile diskFile; 
	private BlockReadBuffer readBuffer;
	private Object readBufferLock = new Object();
	private final Lock lock = new ReentrantLock();  
	
	Block(Index index, File file, long blockNumber, long baseOffset) throws IOException{   
		this.index = index;
		this.blockNumber = blockNumber;
		this.baseOffset = baseOffset;
		this.index.checkBlockNumber(blockNumber);
		
		if(!file.exists()){
			File dir = file.getParentFile();
			if(!dir.exists()){
				dir.mkdirs();
			}  
		}   
		
		this.diskFile = new RandomAccessFile(file,"rw");    
	}   
	
	public int write(DiskMessage... msg) throws IOException {  
		int size = 0;
		for(DiskMessage data : msg){
			size += data.size();
		}
		try{
			lock.lock();
			
			int start = endOffset(); 
			if(start >= Index.BlockMaxSize){
				return 0;
			}  
			 
			ByteBuffer buf = ByteBuffer.allocate(size); 
			long messageNumber = index.getMessageCount();
			int endOffset = start;
			for(DiskMessage data : msg){
				writeToBuffer(data, buf, endOffset, messageNumber++);
				endOffset += data.size();
			} 
			
			diskFile.seek(start);
			diskFile.write(buf.array()); 
			
			index.writeEndOffset(endOffset); 
			index.increaseMessageCount(msg.length);
			
			index.newDataAvailable.get().countDown();
			index.newDataAvailable.set(new CountDownLatch(1)); 
		
			return size;
		} finally {
			lock.unlock();
		}
	}
	
	private ByteBuffer writeChecksumPart(DiskMessage data, long messageNumber){
		ByteBuffer buf = ByteBuffer.wrap(new byte[DiskMessage.CHECKSUM_SIZE]); 
		
		byte[] id = new byte[40]; 
		if(data.id != null){
			id[0] = (byte)data.id.length();
			System.arraycopy(data.id.getBytes(), 0, id, 1, id[0]); 
		} else {
			id[0] = 0; 
		}
		buf.put(id); 
		if(data.timestamp == null){
			buf.putLong(System.currentTimeMillis()); 
		} else {
			buf.putLong(data.timestamp);
		} 
		buf.putLong(messageNumber); //write message number
		
		byte[] tag = new byte[128];
		if(data.tag != null){
			tag[0] = (byte)data.tag.length();
			System.arraycopy(data.tag.getBytes(), 0, tag, 1, tag[0]);
		} else { 
			tag[0] = 0; 
		}
		buf.put(tag);
		
		return buf;
	}
	
	private void writeToBuffer(DiskMessage data, ByteBuffer buf, int endOffset, long messageNumber) {  
		buf.putLong(baseOffset+endOffset);
		ByteBuffer checkedBuf = writeChecksumPart(data, messageNumber);
		long checksum = BlockReadBuffer.calcChecksum(checkedBuf.array());
		buf.putLong(checksum);
		buf.put(checkedBuf.array()); 
		
		if(data.body != null){
			buf.putInt(data.body.length);
			buf.put(data.body);  
		} else {
			buf.putInt(0);  
		}   
	}
	
	private void checkReadBuffer(){
		if(this.readBuffer != null){
			return;
		}
		synchronized (readBufferLock) {
			if(this.readBuffer == null){
				this.readBuffer = new BlockReadBuffer(this.diskFile);
			}
		}
	}
	 
	private DiskMessage readHeadUnsafe(int pos) throws IOException{
		checkReadBuffer();
    	
		DiskMessage data = new DiskMessage();  
    	
    	readBuffer.seek(pos);  
		data.offset = readBuffer.readLong(); //offset
		data.checksum = readBuffer.readLong();
		boolean valid = readBuffer.checksum(DiskMessage.CHECKSUM_SIZE, data.checksum); 
		if(!valid){
			throw new IllegalStateException("read position="+pos+" invalid");
		}
		byte[] id = new byte[40];
		readBuffer.read(id); 
		int idLen = id[0];
		if(idLen>0 && idLen < 40){
			data.id = new String(id, 1, idLen);  
		} else {
			throw new IllegalStateException("Message.Id invalid length");
		}
		data.timestamp = readBuffer.readLong();
		data.messageNumber = readBuffer.readLong();
		byte[] tag = new byte[128];
		readBuffer.read(tag);
		int tagLen = tag[0];
		if(tagLen > 0){ 
			data.tag = new String(tag, 1, tagLen);  
		}  
		data.bytesScanned = DiskMessage.BODY_POS;
		return data; 
	}
	 
    private DiskMessage readFullyUnsafe(int pos) throws IOException{     
    	DiskMessage data = readHeadUnsafe(pos);  
		int size = readBuffer.readInt();
		data.bytesScanned = DiskMessage.BODY_POS + 4;
		if(size > 0){
			byte[] body = new byte[size];
			readBuffer.read(body);
			data.body = body;
			data.bytesScanned += size;
		}
		return data; 
    }
    
    public DiskMessage readHead(int pos) throws IOException{
    	try{
			lock.lock();
			return readHeadUnsafe(pos);
    	} finally {
			lock.unlock();
		}
    }
    
    public DiskMessage readFully(int pos) throws IOException{ 
    	try{
			lock.lock();
			return readFullyUnsafe(pos);
    	} finally {
			lock.unlock();
		}
    }
    
    protected static boolean isMatched(List<String[]> tagPartsList, String target){
    	if(target == null){
    		if(tagPartsList.isEmpty()) return true;
    		return false;
    	}
    	String[] targetParts = target.split("[.]");
    	for(String[] tagParts : tagPartsList){
    		if(isMatched(tagParts, targetParts)){
    			return true;
    		}
    	}
    	return false;
    }
     
    protected static boolean isMatched(String[] tagParts, String[] targetParts){ 
    	
    	for(int i=0;i<tagParts.length;i++){
    		String tagPart = tagParts[i];
    		if(i >= targetParts.length){
    			if(tagParts[i].equals("#")) return true; //last #
    			return false;
    		}
    		String targetPart = targetParts[i];
    		if("*".equals(tagPart)){
    			continue;
    		}
    		if("#".equals(tagPart)){
    			return true;
    		}
    		if(targetPart.equals(tagPart)){
    			continue;
    		} 
    		return false;
    	} 
    	return targetParts.length == tagParts.length;
    }
    
    public DiskMessage readByFilter(int pos, List<String[]> filterParts) throws IOException{ 
    	try{
			lock.lock(); 
			if(filterParts.size() < 1){ 
				return readFullyUnsafe(pos);
			}
			
			int bytesScanned = 0;
			long messageCount = 0;
			while(!isEndOfBlock(pos+bytesScanned)){
				DiskMessage data = readHeadUnsafe(pos+bytesScanned); 
				messageCount = data.messageNumber;
				int size = readBuffer.readInt();
				bytesScanned += data.bytesScanned+4+size; 
				if(!isMatched(filterParts, data.tag)){ 
					int n = readBuffer.skipBytes(size);
					if( n != size){
						throw new IllegalStateException("DiskMessage format error: " + data.offset);
					}
					continue;
				}
				
				if(size > 0){
					byte[] body = new byte[size];
					readBuffer.read(body);
					data.body = body; 
				}
				data.bytesScanned = bytesScanned; 
				return data;
			}
			
			DiskMessage data = new DiskMessage();
			data.messageNumber = messageCount;
			data.valid = false;
			data.bytesScanned = bytesScanned;
			return data;
    	} finally {
			lock.unlock();
		}
    } 
    
    /**
     * Check if endOffset of block reached max block size allowed
     * @return true if max block size reached, false other wise
     * @throws IOException 
     */
    public boolean isFull() throws IOException{
    	return endOffset() >= Index.BlockMaxSize;
    }
    
    /**
     * Check if offset reached the end, for read.
     * @param offset offset of reading
     * @return true if reached the end of block(available data), false otherwise
     * @throws IOException 
     */
    public boolean isEndOfBlock(int offset) throws IOException{  
    	return offset >= endOffset();
    }
    
    private int endOffset() throws IOException{
    	return index.readOffset(blockNumber).endOffset;
    } 
    
    public long getBlockNumber() {
		return blockNumber;
	}
    
	@Override
	public void close() throws IOException {  
		this.diskFile.close();
	}  
}
