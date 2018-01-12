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
package io.zbus.rpc;
 

import io.zbus.kit.HttpKit;
import io.zbus.kit.JsonKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.transport.http.Message; 
 

public class JsonRpcCodec implements RpcCodec {  
	private static final String DEFAULT_ENCODING = "UTF-8"; 
	private boolean requestTypeInfo = true;   
	private boolean responseTypeInfo = false; //Browser friendly 

	public void setRequestTypeInfo(boolean requestTypeInfo) {
		this.requestTypeInfo = requestTypeInfo;
	} 
	
	public void setResponseTypeInfo(boolean responseTypeInfo) {
		this.responseTypeInfo = responseTypeInfo;
	} 

	public Message encodeRequest(Request request, String encoding) {
		Message msg = new Message();  
		if(encoding == null) encoding = DEFAULT_ENCODING;  
		msg.setEncoding(encoding);
		if(requestTypeInfo) {
			msg.setBody(JsonKit.toJSONBytesWithType(request, encoding)); 
		} else {
			msg.setBody(JsonKit.toJSONBytes(request, encoding));
		}
		String contentType = "application/json";
		msg.setHeader("content-type", contentType);
		return msg;
	}
	private Request decodeUpload(Message msg) {
		UrlInfo info = HttpKit.parseUrl(msg.getUrl());  
		if(info.path.size() < 1){
			return null;
		}
		
		int moduleStart = 0;
		if(msg.getHeader("topic") != null) moduleStart = 1; //MQ based, topic prefix added
		
		Request req = new Request();
    	if(info.path.size()>=moduleStart+1){
    		req.setModule(info.path.get(moduleStart));
    	}
    	if(info.path.size()>=moduleStart+2){
    		req.setMethod(info.path.get(moduleStart+1));
    	} 
    	
    	int paramStart = moduleStart+2;
    	if(info.path.size()>paramStart){
    		Object[] params = new Object[info.path.size()-(paramStart)];
    		for(int i=0;i<params.length;i++){
    			params[i] = info.path.get(paramStart+i);
    		}
    		req.setParams(params); 
    	}   
    	return req;
	}
	public Request decodeRequest(Message msg) {
		String encoding = msg.getEncoding();
		if(encoding == null){
			encoding = DEFAULT_ENCODING;
		}
		String contentType = msg.getHeader(Message.CONTENT_TYPE);
		if(contentType != null && contentType.startsWith(Message.CONTENT_TYPE_UPLOAD)){
			return decodeUpload(msg);
		} 
		
		String jsonString = msg.getBodyString(encoding);
		return JsonKit.parseObject(jsonString, Request.class);  
	} 
	
	public Message encodeResponse(Object response, String encoding) {
		Message msg = new Message();   
		if(encoding == null) encoding = DEFAULT_ENCODING;  
		msg.setEncoding(encoding);  
		if(responseTypeInfo) {
			msg.setBody(JsonKit.toJSONBytesWithType(response, encoding)); 
		} else {
			msg.setBody(JsonKit.toJSONBytes(response, encoding));
		} 
		String contentType = "application/json";
		msg.setHeader("content-type", contentType);
		return msg; 
	}
	
 
	public Object decodeResponse(Message msg){ 
		String encoding = msg.getEncoding();
		if(encoding == null){
			encoding = DEFAULT_ENCODING;
		}
		String jsonString = msg.getBodyString(encoding);
		Object res = null; 
		try{
			res = JsonKit.parseObject(jsonString, Object.class);
		} catch (Exception e){  
			try{
				jsonString = jsonString.replace("@type", "@class"); //trick: disable desearialization by class name
				res = JsonKit.parseObject(jsonString); 
			} catch(Exception ex){
				String prefix = "";
				if(msg.getStatus() == 200){ 
					prefix = "JSON format invalid: ";
				}
				throw new RpcException(prefix + jsonString);
			}  
		} 
		return res;
	} 
	
	@Override
	public <T> T convert(Object value, Class<T> clazz) {
		return JsonKit.convert(value, clazz);
	}
}
