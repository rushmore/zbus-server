/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.zbus.transport;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;


public class Pool<T> implements Closeable { 
	public static interface ObjectFactory<T> {
		
		T createObject() throws Exception;
		
		void destroyObject(T obj);
		
		boolean validateObject(T obj);
	}
	
	private ObjectFactory<T> factory;  
	private BlockingQueue<T> queue = null;
	private final int maxTotal;
	private final AtomicInteger activeCount = new AtomicInteger(0);
	
	public Pool(ObjectFactory<T> factory, int maxTotal) { 
		this.factory = factory; 
		
		this.maxTotal = maxTotal; 
		this.queue = new ArrayBlockingQueue<T>(maxTotal);
	}
	
	@Override
	public void close() throws IOException { 
		T obj = null;
		while((obj = queue.poll()) != null){
			factory.destroyObject(obj);
		}
	}
 
	public T borrowObject() throws Exception { 
		T  obj = null;
		if(activeCount.get() >= maxTotal){
			obj = queue.take();
			return obj;
		}
		obj = queue.poll();
		if(obj != null) return obj;
		
		obj = factory.createObject();
		activeCount.incrementAndGet(); 
		
		return obj; 
	}
 
	public void returnObject(T obj) { 
		if(!factory.validateObject(obj)){
			activeCount.decrementAndGet();
			factory.destroyObject(obj); 
			return;
		}
		queue.offer(obj);
	}
}
