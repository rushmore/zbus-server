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
package io.zbus.util.pool;

import java.io.Closeable;

import io.zbus.util.pool.impl.DefaultPoolFactory;


public abstract class Pool<T> implements Closeable { 
	
	public abstract T borrowObject() throws Exception;
	
	public abstract void returnObject(T obj);

	public static <T> Pool<T> getPool(ObjectFactory<T> factory, PoolConfig config){
		return Pool.factory.getPool(factory, config);
	}
	
	
	private static PoolFactory factory;
	
	static {
		initDefaultFactory();
	} 
	
	public static void setPoolFactory(PoolFactory factory) {
		if (factory != null) {
			Pool.factory = factory;
		}
	}
	
	public static void initDefaultFactory() {
		if (factory != null){
			return ;
		}
		String defaultFactory = String.format("%s.impl.CommonsPool2Factory", Pool.class.getPackage().getName());
		try {
			//try commons-pool2
			Class.forName("org.apache.commons.pool2.BasePooledObjectFactory");
			Class<?> factoryClass = Class.forName(defaultFactory);
			factory = (PoolFactory)factoryClass.newInstance();
		} catch (Exception e) { 
			factory = new DefaultPoolFactory();
		}
	}
}
