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
package io.zbus.kit.logging.impl;
 

import org.slf4j.spi.LocationAwareLogger;

import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory.InternalLoggerFactory;

public class Sl4jLoggerFactory implements InternalLoggerFactory {
	
	public Logger getLogger(Class<?> clazz) {
		return new Sl4jLogger(clazz);
	}
	
	public Logger getLogger(String name) {
		return new Sl4jLogger(name);
	} 
	
	public static void main(String[] args){
		Logger log = new Sl4jLogger(Sl4jLoggerFactory.class);
		log.info("test");
	}
}

class Sl4jLogger extends Logger { 
	private org.slf4j.Logger log; 
	private final String FQCN = Sl4jLogger.class.getName();
	
	Sl4jLogger(Class<?> clazz) { 
		log = org.slf4j.LoggerFactory.getLogger(clazz);
	}
	
	Sl4jLogger(String name) {
		log = org.slf4j.LoggerFactory.getLogger(name);
	}
	
	public void info(String message) { 
		info(message, (Throwable)null);
	}
	
	public void info(String message, Throwable t) {
		if (log instanceof LocationAwareLogger) {
	        ((LocationAwareLogger) log).log(null, FQCN, 
	        		LocationAwareLogger.INFO_INT, message, null, t);
	    } else {
	        log.info(message, t);
	    } 
	}
	
	public void debug(String message) {
		debug(message, (Throwable)null);
	}
	
	public void debug(String message, Throwable t) {
		if (log instanceof LocationAwareLogger) {
	        ((LocationAwareLogger) log).log(null, FQCN, 
	        		LocationAwareLogger.DEBUG_INT, message, null, t);
	    } else {
	        log.debug(message, t);
	    } 
	}
	
	public void warn(String message) {
		warn(message, (Throwable)null);
	}
	
	public void warn(String message, Throwable t) {
		if (log instanceof LocationAwareLogger) {
	        ((LocationAwareLogger) log).log(null, FQCN, 
	        		LocationAwareLogger.WARN_INT, message, null, t);
	    } else {
	        log.warn(message);
	    } 
	}
	
	public void error(String message) {
		error(message, (Throwable)null);
	}
	
	public void error(String message, Throwable t) {
		if (log instanceof LocationAwareLogger) {
	        ((LocationAwareLogger) log).log(null, FQCN, 
	        		LocationAwareLogger.ERROR_INT, message, null, t);
	    } else {
	        log.error(message);
	    } 
	}
	
	public void fatal(String message) {
		error(message);
	}
	
	public void fatal(String message, Throwable t) {
		error(message, t);
	}
	
	public boolean isDebugEnabled() {
		return log.isDebugEnabled();
	}
	
	public boolean isInfoEnabled() {
		return log.isInfoEnabled();
	}
	
	public boolean isWarnEnabled() {
		return log.isWarnEnabled();
	}
	
	public boolean isErrorEnabled() {
		return log.isErrorEnabled();
	}
	
	public boolean isFatalEnabled() {
		return log.isErrorEnabled();
	}

	@Override
	public void trace(String message) {
		trace(message, (Throwable)null);	}

	@Override
	public void trace(String message, Throwable t) {
		if (log instanceof LocationAwareLogger) {
	        ((LocationAwareLogger) log).log(null, FQCN, 
	        		LocationAwareLogger.TRACE_INT, message, null, t);
	    } else {
	        log.error(message);
	    } 
	}

	@Override
	public boolean isTraceEnabled() { 
		return log.isTraceEnabled();
	}
}