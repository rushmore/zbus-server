package io.zbus.mq.disk;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
 
public class QueueNak extends MappedFile { 
	private static final int MaxWindowCount = 1000;   
	private static final int FileSize = HeadSize + MaxWindowCount * NakRecord.Size;  
	private static final int WindowPos = 0;  
	private static final int TimeoutPos = 4;  
	
	private int window = 100; 
	private long timeout = TimeUnit.SECONDS.toMillis(10); //default to 10s 
	
	private Queue<NakRecord> queue = new PriorityQueue<NakRecord>(window, new NakRecordComparator());
	private Queue<Integer> availableEntries = new PriorityQueue<Integer>(); 
	
	
	
	private final QueueReader queueReader;
	
	public QueueNak(QueueReader queueReader) throws IOException{ 
		this.queueReader = queueReader; 
		File nakFile = new File(queueReader.getReaderDir(), queueReader.getGroupName() + Index.NakSuffix);
		load(nakFile, FileSize); 
	}   
	
	public int size() {
		return queue.size();
	}
	
	public int remaining() {
		return window - size();
	}
	
	public int getWindow() {
		return this.window;
	}
	
	public void setWindow(int value) {
		if(value < 0 || value>=MaxWindowCount) {
			throw new IllegalArgumentException("nakLimit(" + value + ") invalid");
		}
		this.window = value;
		try {
			lock.lock(); 
			buffer.position(WindowPos);
			buffer.putInt(this.window);
		} finally {
			lock.unlock();
		}
	}
	
	public long getTimeout() {
		return this.timeout;
	}
	
	public void setTimeout(long value) { 
		this.timeout = value;
		try {
			lock.lock(); 
			buffer.position(TimeoutPos);
			buffer.putLong(this.timeout);
		} finally {
			lock.unlock();
		}
	}
	
	public Iterator<NakRecord> iterator(){
		return queue.iterator();
	}
	
	public void clear() {
		buffer.position(HeadSize);
		buffer.put(new byte[NakRecord.Size*MaxWindowCount]);
		queue.clear(); 
		
		for(int i=0;i<MaxWindowCount;i++) {
			availableEntries.add(i);
		}
	} 
	
	public NakRecord getNak(long offset) {
		Iterator<NakRecord> iter = iterator();
		while(iter.hasNext()) {
			NakRecord nak = iter.next();
			if(nak.offset == offset) {
				return nak;
			}
		}
		return null;
	} 
	
	public NakRecord pollTimeoutNak() {
		synchronized(queue) {
			NakRecord nak = queue.peek();
			if(nak == null) return null;
			if(System.currentTimeMillis() >= (nak.updatedTime+timeout)) {
				removeNak(nak);
				return queue.poll();
			} 
		} 
		return null;
	}
	
	public TimeoutMessage pollTimeoutMessage() throws IOException {
		NakRecord nak = pollTimeoutNak();
		if(nak == null) return null;
 
		DiskMessage msg = queueReader.read(nak.offset); 
		return new TimeoutMessage(msg, nak);
	}
	
	public static class TimeoutMessage{
		public DiskMessage diskMessage;
		public NakRecord nakRecord;
		
		public TimeoutMessage(DiskMessage diskMessage, NakRecord nakRecord){
			this.diskMessage = diskMessage;
			this.nakRecord = nakRecord;
		}
	}
	
	public void addNak(long offset, Integer retry) {
		try {
			lock.lock();  
			
			NakRecord nak = getNak(offset);
			if(nak != null) { 
				nak.updatedTime = System.currentTimeMillis();
				writeNakUnsafe(nak);
				return;
			}
			
			if(queue.size() > window) {
				throw new IllegalStateException("NAK queue full");
			}
			nak = new NakRecord(); 
			Integer entryNumber = availableEntries.poll();
			if(entryNumber == null) {
				throw new IllegalStateException("NAK entry full");
			}
			nak.entryNumber = entryNumber;
			nak.offset = offset; 
			nak.status = 1;
			nak.retryCount = retry==null? 1 : retry+1;
			
			queue.add(nak);
			
			writeNakUnsafe(nak);
		} finally {
			lock.unlock();
		}
	}
	
	public void removeNak(long offset) { 
		NakRecord nak = null;
		Iterator<NakRecord> iter = iterator();
		while(iter.hasNext()) {
			NakRecord t = iter.next();
			if(t.offset == offset) { 
				nak = t;
				break;
			}
		}
		if(nak == null) return;
		
		removeNak(nak);
		iter.remove(); 
	}
	
	public void removeNak(NakRecord nak) {
		try {
			lock.lock();  
			nak.status = 0;
			availableEntries.add(nak.entryNumber);
			writeNakUnsafe(nak);
		} finally {
			lock.unlock();
		}
	}

	@Override
	protected void loadDefaultData() throws IOException {  
		buffer.position(WindowPos);
		this.window = buffer.getInt();
		this.timeout = buffer.getLong();
		
		for(int i=0; i< MaxWindowCount; i++) {
			NakRecord nak = readNakUnsafe(i);
			if(nak.status != 1) {
				availableEntries.add(i);
				continue;
			}
			queue.add(nak); 
		}
	}

	@Override
	protected void writeDefaultData() throws IOException { 
		buffer.position(WindowPos);
		buffer.putInt(this.window);
		buffer.putLong(this.timeout);
		
		for(int i=0;i<MaxWindowCount;i++) {
			availableEntries.add(i);
		}
	}   
	
	private int nakPosition(int entryNumber) {
		return HeadSize + entryNumber*NakRecord.Size;
	}
	
	private void writeNakUnsafe(NakRecord nak) {
		int pos = nakPosition(nak.entryNumber);
		buffer.position(pos); 
		buffer.put(nak.status);
		buffer.putLong(nak.offset); 
		buffer.putInt(nak.retryCount);
		buffer.putLong(nak.updatedTime); 
	} 
	
	private NakRecord readNakUnsafe(int entryNumber) throws IOException { 
		NakRecord nak = new NakRecord();
		nak.entryNumber = entryNumber;
	
		int pos = nakPosition(entryNumber);
		buffer.position(pos);  
		nak.offset = buffer.getLong(); 
		nak.retryCount = buffer.getInt();
		nak.updatedTime = buffer.getLong(); 
		
		return nak;
	}

	public NakRecord readNak(int entryNumber) throws IOException { 
		try {
			lock.lock();
			return readNakUnsafe(entryNumber);
		} finally {
			lock.unlock();
		}
	}
	
	public static class NakRecord {
		public byte status;     //1   0 - invalid, 1 - valid
		public long offset;	    //8 
		public int retryCount;  
		public long updatedTime = System.currentTimeMillis(); 
		
		public int entryNumber; //entry number, offset from 0, not persisted

		public final static int Size = 1 + 8 + 4 + 8; //21
	}
	
	private static class NakRecordComparator implements Comparator<NakRecord>{

		@Override
		public int compare(NakRecord o1, NakRecord o2) {  
			if(o1.updatedTime > o2.updatedTime) return 1;
			if(o1.updatedTime == o2.updatedTime) return 0;
			return -1;
		}
		
	}
}
