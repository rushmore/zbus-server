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
package io.zbus.kit;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class ConfigKit {    
	
	public static String option(Properties props, String opt, String defaultValue){
		String value = props.getProperty(opt, defaultValue);
		return value == null? null : value.trim();
	}
	
	public static int option(Properties props, String opt, int defaultValue){
		String value = option(props, opt, null);
		if(value == null) return defaultValue;
		return Integer.valueOf(value);
	}
	
	public static String option(String[] args, String opt, String defaultValue){
		for(int i=0; i<args.length;i++){
			if(args[i].equals(opt)){
				if(i<args.length-1) return args[i+1];
				else return null;
			} 
		}
		return defaultValue;
	}
	
	public static int option(String[] args, String opt, int defaultValue){
		String value = option(args, opt, null);
		if(value == null) return defaultValue;
		return Integer.valueOf(value);
	}
	
	public static boolean option(String[] args, String opt, boolean defaultValue){
		String value = option(args, opt, null);
		if(value == null) return defaultValue;
		return Boolean.valueOf(value);
	}
	
	private static Set<String> split(String value){
		Set<String> res = new HashSet<String>();
		String[] blocks = value.split("[,]");
		for(String b : blocks){
			b = b.trim();
			if("".equals(b)) continue;
			res.add(b);
		}
		return res;
	} 
	
	public static String value(Properties props, String name, String defaultValue){ 
		return props.getProperty(name, defaultValue).trim();
	}
	public static int value(Properties props, String name, int defaultValue){ 
		String value = value(props, name, "");
		if("".equals(value)) return defaultValue;
		return Integer.valueOf(value);
	}
	
	public static boolean value(Properties props, String name, boolean defaultValue){ 
		String value = value(props, name, "");
		if("".equals(value)) return defaultValue;
		return Boolean.valueOf(value);
	}
	public static String value(Properties props, String name){
		return value(props, name, "");
	}
	
	public static Set<String> valueSet(Properties props, String name){ 
		return split(value(props, name));
	} 

	public static Properties loadConfig(String fileName){ 
		Properties props = new Properties();
		try{
			InputStream fis = FileKit.loadFile(fileName);
			if(fis != null){
				props.load(fis);
			}
		} catch(Exception e){ 
			System.out.println("Missing config, using default empty");
		}
		return props;
	}
	
	public static String valueOf(String value, String defaultValue){
		if(StrKit.isEmpty(value)) return defaultValue;
		return value.trim();
	}
	
	public static int valueOf(String value, int defaultValue){
		if(StrKit.isEmpty(value)) return defaultValue;
		return Integer.valueOf(value);
	}
	
	public static boolean valueOf(String value, boolean defaultValue){
		if(StrKit.isEmpty(value)) return defaultValue;
		return Boolean.valueOf(value);
	}   
	
}
