package io.zbus.mq.memory;

public class CircularArray {
	final int maxSize;
	long start = 0; // readable entry
	long end = 0;   // first entry to write
	Object[] array;

	public CircularArray(int maxSize) {
		this.maxSize = maxSize;
		array = new Object[this.maxSize];
	}

	public CircularArray() {
		this(10000);
	}

	public int size() {
		synchronized (array) {
			return (int) (end - start);
		}
	}

	private int forwardIndex() {
		if (end - start >= maxSize) {
			start++;
		}
		end++;
		return (int) (end % maxSize);
	}

	public void write(Object... data) {
		synchronized (array) {
			for (Object obj : data) {
				int i = (int) (end % maxSize);
				array[i] = obj;
				forwardIndex();
			}
		}
	}  
}
