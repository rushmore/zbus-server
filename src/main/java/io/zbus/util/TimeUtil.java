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
package io.zbus.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class TimeUtil { 
	public static long parseTimeWithUnit(String time){
    	time = time.trim();
    	long value = 0;
    	long unit = 0; 
    	int unitLength = 1;
    	 
    	if(time.endsWith("s")){
    		unit = 1000; 
    	} 
    	if(time.endsWith("ms")){
    		unit = 1;
    		unitLength = 2;
    	} else if (time.endsWith("m")){ 
    		unit = 60*1000; 
    	} else if (time.endsWith("h")){
    		unit = 60*60*1000; 
    	} else if (time.endsWith("d")){
    		unit = 24*60*60*1000; 
    	} 
    	
    	if(unit != 0){
    		value = Long.valueOf(time.substring(0, time.length()-unitLength));
    		value *= unit;
    		return value;
    	}
    	throw new IllegalArgumentException(time + " missing unit");
    	
	}
	
	
	public static long parseDelayTime(String time){
    	time = time.trim();
    	try{
    		return parseTimeWithUnit(time);
    	} catch (IllegalArgumentException e){
    		long value = 0;
    		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    		try {
    			value = format.parse(time).getTime(); 
    		} catch (ParseException e1) { 
    			format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    			try {
    				value = format.parse(time).getTime();
    			} catch (ParseException e2) {
    				throw new IllegalArgumentException(time, e2);
    			} 
    		}
    		value -= System.currentTimeMillis();
    		return value;
    	} 
    }
	
	public static void main(String[] args) {
		//delay: 10s, 20s, 30s, 2016-03-01 9:40:36
		//ttl: 100s
		
		long value = parseDelayTime("2016-03-24 15:40:36");
		System.out.println(value);
	}
}
