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

public class HttpKit {

	public static class UrlInfo {
		public List<String> path = new ArrayList<String>();
		public Map<String, String> params = new HashMap<String, String>(); 
	}
	
	public static UrlInfo parseUrl(String url){
		UrlInfo info = new UrlInfo();
		if("/".equals(url) || StrKit.isEmpty(url)){
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
    		if(StrKit.isEmpty(b)) continue;
    		info.path.add(b);
    	}
    	
    	if(params != null){
    		info.params = StrKit.kvp(params, "&");
    	} 
		return info;
	}
	
	public static String rpcUrl(String url, boolean hasTopic){
		String[] bb = url.split("[/]");
		String resource = "";
		int count = 0;
		int prefixCount = 3;
		if(hasTopic) prefixCount = 4;
		for(int i=0;i<bb.length;i++){
			if(bb[i].equals("")) continue;
			count++;
			if(count<prefixCount) continue;
			resource += bb[i];
			if(i<bb.length-1) resource+= "/";
		}
		return resource;
	}
	
	public static String contentType(String resource){
		if(resource.endsWith(".js")) {
			return "application/javascript";
		} 
		if(resource.endsWith(".css")) {
			return "text/css";
		} 
		if(resource.endsWith(".htm") || resource.endsWith(".html")){ 
			return "text/html";
		}
		if(resource.endsWith(".svg")){
			return "image/svg+xml";
		} 
		if(resource.endsWith(".gif")){
			return "image/gif";
		}
		if(resource.endsWith(".jpeg")){
			return "image/jpeg";
		}
		if(resource.endsWith(".ico")){
			return "image/x-icon";
		}  
		if(resource.endsWith(".png")){
			return "image/png";
		}  
		if(resource.endsWith(".pdf")){
			return "application/pdf";
		}  
		if(resource.endsWith(".zip")){
			return "application/zip";
		}  
		
		return null; 
	}
}
