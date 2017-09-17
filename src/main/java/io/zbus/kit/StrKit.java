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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StrKit {

	public static boolean isEmpty(String str) {
		return str == null || str.trim().equals(""); 
	} 
	
	public static Object[] hostPort(String address){
		Object[] res = new Object[2];
		String[] bb = address.split("[:]",2);
		if(bb.length > 0){
			res[0] = bb[0].trim();
		}
		res[1] = 80;
		if(bb.length>1){
			res[1] = Integer.valueOf(bb[1]); 
		}
		return res;
	}
	
	public static Map<String, String> kvp(String value){
		return kvp(value, "&");
	}
	
	public static Map<String, String> kvp(String value, String delim){
		if(isEmpty(delim)) {
			delim = "[ ;]";
		} 
		
		Map<String, String> res = new HashMap<String, String>();
		if(isEmpty(value)) return res;
		
		
		String[] kvs = value.split(delim);
		for(String kv : kvs){
			kv = kv.trim();
			if(kv.equals("")) continue;
			String[] bb = kv.split("=");
			String k="",v="";
			if(bb.length > 0){
				k = bb[0].trim();
			}
			if(bb.length > 1){
				v = bb[1].trim();
			}
			res.put(k, v);
		}
		return res;
	}
	
	public static class UrlInfo {
		public List<String> path = new ArrayList<String>();
		public Map<String, String> params = new HashMap<String, String>(); 
	}
	
	public static UrlInfo parseUrl(String url){
		UrlInfo info = new UrlInfo();
		if("/".equals(url) || isEmpty(url)){
			return info;
		} 
    	String path = url;
    	String params = null;
    	int idx = url.indexOf('?');
    	if(idx >= 0){
    		path = url.substring(0, idx);  
    		params = url.substring(idx+1);
    	} 
    	String[] bb = path.split("/");
    	for(String b : bb){
    		if(isEmpty(b)) continue;
    		info.path.add(b);
    	}
    	
    	if(params != null){
    		info.params = kvp(params, "&");
    	} 
		return info;
	} 
}
