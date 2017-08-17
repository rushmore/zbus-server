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
package io.zbus.kit.logging;

import io.zbus.kit.logging.impl.JdkLoggerFactory;

public class LoggerFactory {
	public static interface InternalLoggerFactory {
		Logger getLogger(Class<?> clazz);
		Logger getLogger(String name);
	}
	
	private static InternalLoggerFactory factory;
	static {
		initDefaultFactory();
	}
	
	public static void setLoggerFactory(InternalLoggerFactory factory) {
		if (factory != null) {
			LoggerFactory.factory = factory;
		}
	}
	
	public static Logger getLogger(Class<?> clazz) {
		return factory.getLogger(clazz);
	}
	
	public static Logger getLogger(String name) {
		return factory.getLogger(name);
	}
	
	
	public static void initDefaultFactory() {
		if (factory != null){
			return ;
		}
		
		try {
			//default to Log4j
			Class.forName("org.apache.log4j.Logger");
			String defaultFactory = String.format("%s.impl.Log4jLoggerFactory", Logger.class.getPackage().getName());
			Class<?> factoryClass = Class.forName(defaultFactory);
			factory = (InternalLoggerFactory)factoryClass.newInstance();
			return;
		} catch (Exception e) {  
		}
		
		try {
			//try slf4j
			Class.forName("org.slf4j.Logger");
			String defaultFactory = String.format("%s.impl.Sl4jLoggerFactory", Logger.class.getPackage().getName());
			Class<?> factoryClass = Class.forName(defaultFactory);
			factory = (InternalLoggerFactory)factoryClass.newInstance();
			return;
		} catch (Exception e) { 
		} 
		
		if(factory == null){
			factory = new JdkLoggerFactory();
		}
	} 

}
