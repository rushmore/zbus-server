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
package io.zbus.transport.http;
 

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.transport.Id;

public class Message implements Id {  
	private static final Logger log = LoggerFactory.getLogger(Message.class); 
	private static final String DEFAULT_ENCODING = "UTF-8"; 
	
	public static final String HEARTBEAT        = "heartbeat";  
	public static final String ID      		    = "id";	  
	public static final String ENCODING      	= "encoding";	 
	public static final String CMD      	    = "cmd";	
	
	public static final String REMOTE_ADDR      = "remote-addr";
	public static final String CONTENT_LENGTH   = "content-length";
	public static final String CONTENT_TYPE     = "content-type";    
	public static final String CONTENT_TYPE_BINARY   = "application/octet-stream"; 
	public static final String CONTENT_TYPE_JSON     = "application/json"; 
	public static final String CONTENT_TYPE_UPLOAD   = "multipart/form-data";
	 
	protected Integer status; //null: request, otherwise: response
	protected String url = "/";
	protected String method = "GET"; 
	
	protected Map<String, String> headers = new ConcurrentHashMap<String, String>(); 
	protected byte[] body; 
	
	protected FileForm fileForm;  //Only populated when uploading files
	
	public Message(){
		setBody((byte[])null);
		setCommonHeaders();
	} 
	
	public Message(Message other){ 
		this.method = other.method;
		this.status = other.status;
		this.url = other.url;
		
		this.headers = other.headers;
		this.body = other.body;
		this.fileForm = other.fileForm;
	}
	
	public Message(String body){
		setBody(body); 
		setCommonHeaders();
	}
	
	public Message(byte[] body){
		setBody(body);
		setCommonHeaders();
	}
	
	protected void setCommonHeaders(){
		setHeader("connection", "Keep-Alive"); 
	}
	
	public static Message copyWithoutBody(Message msg){
		Message res = new Message();
		res.status = msg.status;
		res.url = msg.url;
		res.method = msg.method;
		res.headers = new ConcurrentHashMap<String, String>(msg.headers);
		res.body = msg.body;
		res.fileForm = msg.fileForm;
		return res;
	}
	 
	public String getUrl(){
		return this.url;
	} 
	
	public Message setUrl(String url){ 
		this.url = url;  
		return this;
	}
	
	public Message setStatus(Integer status) { 
		this.status = status;
		return this; 
	} 
	
	public Integer getStatus(){
		return status;
	} 
	
	public String getMethod(){
		return this.method;
	}
	
	public void setMethod(String method){
		this.method = method;
	} 
	
	public Map<String,String> getHeaders() {
		return headers;
	} 
	
	public void setHeaders(Map<String,String> head) {
		this.headers = head;
	} 
	
	public String getHeader(String key){
		return this.headers.get(key);
	}
	
	public String getHeader(String key, String defaultValue){
		String res = this.headers.get(key);
		if(res == null) return defaultValue;
		return res;
	}
	
	public void setHeader(String key, String value){
		if(value == null) return;
		this.headers.put(key, value);
	} 
	
	public void setHeader(String key, Object value){
		if(value == null) return;
		this.headers.put(key, value.toString());
	} 
	
	public String removeHeader(String key){
		return this.headers.remove(key);
	}
	
	public byte[] getBody() {
		byte[] b = body;
		String bodyOfHead = getHeader("body");
		if(b == null && bodyOfHead != null){
			b = bodyOfHead.getBytes();
		}
		return b;
	}  
	
	public Message setBody(byte[] body) {
		int len = 0;
		if( body != null){
			len = body.length;
		}
		this.setHeader(CONTENT_LENGTH, ""+len); 
		this.body = body;
		return this;
	}
	
	public Message setBody(String body, String encoding){
		try {
			return setBody(body.getBytes(encoding));
		} catch (UnsupportedEncodingException e) { //just ignore
			return setBody(body);
		}
	}
	
	public Message setBody(String body){ 
		String encoding = this.getEncoding();
		if(encoding == null){
			encoding = DEFAULT_ENCODING;
		}
		try {
			return setBody(body.getBytes(encoding));
		} catch (UnsupportedEncodingException e) { //just ignore
			return setBody(body.getBytes());
		}
	} 
	
	public Message setBody(String format, Object ...args) { 
		this.setBody(String.format(format, args));
		return this;
	}  
	
	public Message setJsonBody(String body){
		return this.setJsonBody(body.getBytes());
	}
	
	public Message setJsonBody(byte[] body){ 
		this.setBody(body);
		this.setHeader(CONTENT_TYPE, CONTENT_TYPE_JSON);
		return this;
	}
	
	public String getBodyString() {
		if (this.getBody() == null) return null;
		String encoding = this.getEncoding();
		if(encoding == null){
			encoding = DEFAULT_ENCODING;
		}
		return getBodyString(encoding);
	}

	public String getBodyString(String encoding) {
		if (this.getBody() == null) return null;
		try {
			return new String(this.getBody(), encoding);
		} catch (UnsupportedEncodingException e) {
			return new String(this.getBody());
		}
	}
	
	public String getId() {
		return this.getHeader(ID);
	} 
	
	public void setId(String msgId) {
		if(msgId == null) return;
		this.setHeader(ID, msgId); 
	}	 
	
	public String getCommand() { 
		return this.getHeader(CMD);
	} 
	
	public Message setCommand(String value) {
		this.setHeader(CMD, value); 
		return this;
	}   
	
	public String getEncoding() {
		return this.getHeader(ENCODING);
	} 
	
	public Message setEncoding(String encoding) {
		this.setHeader(ENCODING, encoding);
		return this;
	} 
	
	//////////////////////////////////////////////////////////////
	public void decodeHeaders(InputStream in){ 
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		try{  
			inputStreamReader = new InputStreamReader(in);
			bufferedReader = new BufferedReader(inputStreamReader);
			String meta = bufferedReader.readLine();
			if(meta == null) return;

			StringTokenizer st = new StringTokenizer(meta);
			String start = st.nextToken();
			if(start.toUpperCase().startsWith("HTTP")){ //As response
				this.status = Integer.valueOf(st.nextToken()); 
			} else {
				this.method = start;  
				this.url = st.nextToken();
			}
			
			String line = bufferedReader.readLine();
	        while (line != null && line.trim().length() > 0) {
	            int p = line.indexOf(':');
	            if (p >= 0){ 
	                headers.put(line.substring(0, p).trim().toLowerCase(), line.substring(p + 1).trim());
	            } 
	            line = bufferedReader.readLine();
	        }
	       
		} catch(IOException e){ 
			log.error(e.getMessage(), e);
		} finally {
			if(bufferedReader != null){
				try { bufferedReader.close(); } catch (IOException e) {}
			}
			if(inputStreamReader != null){
				try { inputStreamReader.close(); } catch (IOException e) {}
			} 
		}
	}
	
	public void decodeHeaders(byte[] data, int offset, int size){
		ByteArrayInputStream in = new ByteArrayInputStream(data, offset, size);
		decodeHeaders(in);
		if(in != null){
			try { in.close(); } catch (IOException e) {}
		} 
	}
	 
	
	protected String getBodyPrintString() {
		if (this.body == null)
			return null;
		if (this.body.length > 1024) {
			return new String(this.body, 0, 1024) + "...";
		} else {
			return getBodyString();
		}
	}
	
	static final byte[] CLCR = "\r\n".getBytes();
	static final byte[] KV_SPLIT = ": ".getBytes();
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(generateHttpLine()+"\r\n");
		
		List<String> keys = new ArrayList<String>(headers.keySet());
		Collections.sort(keys);
		
		for(String key : keys){ 
			String val = headers.get(key);
			sb.append(key+": "+val+"\r\n");
		}
		sb.append("\r\n");
		String bodyString = getBodyPrintString();
		if(bodyString != null){
			sb.append(bodyString);
		}
		return sb.toString();
	} 
	
	public byte[] toBytes(){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			writeTo(out);
		} catch (IOException e) {
			return null;
		}
		return out.toByteArray(); 
	} 
	
	private static int findHeaderEnd(byte[] data){ 
		int i = 0;
		int limit = data.length;
		while(i+3<limit){
			if(data[i] != '\r') {
				i += 1;
				continue;
			}
			if(data[i+1] != '\n'){
				i += 1;
				continue;
			}
			
			if(data[i+2] != '\r'){
				i += 3;
				continue;
			}
			
			if(data[i+3] != '\n'){
				i += 3;
				continue;
			}
			
			return i+3; 
		}
		return -1;
	}
	
	public static Message parse(byte[] data){
		int idx = findHeaderEnd(data);
		if(idx == -1){
			throw new IllegalArgumentException("Invalid input byte array");
		}
		int headLen = idx + 1;
		Message msg = new Message();
		msg.decodeHeaders(data, 0, headLen);
		String contentLength = msg.getHeader(Message.CONTENT_LENGTH);
		if(contentLength == null){ //just head 
			return msg;
		}
		
		int bodyLen = Integer.valueOf(contentLength);   
		if(data.length != headLen + bodyLen) {
			throw new IllegalArgumentException("Invalid input byte array");
		}
		byte[] body = new byte[bodyLen];
		System.arraycopy(data, headLen, body, 0, bodyLen);
		msg.setBody(body); 
		
		return msg; 
	}
	
	private final static byte[] BLANK = " ".getBytes();
	private final static byte[] PREFIX = "HTTP/1.1 ".getBytes();
	private final static byte[] SUFFIX = " HTTP/1.1".getBytes(); 
	
	private String generateHttpLine(){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			writeHttpLine(out);
		} catch (IOException e) { 
			return null;
		}
		return out.toString();
	}
	
	private void writeHttpLine(OutputStream out) throws IOException{
		if(this.status != null){
			String desc = null;
			HttpResponseStatus s = HttpResponseStatus.valueOf(Integer.valueOf(status));
			if(s != null){
				desc = s.reasonPhrase();
			} else {
				desc = "Unknown Status";
			}
			out.write(PREFIX);
			out.write(String.format("%d", status).getBytes());
			out.write(BLANK);
			out.write(desc.getBytes());  
		} else {
			String method = this.method; 
			if(method == null) method = ""; 
			out.write(method.getBytes());
			out.write(BLANK); 
			String requestString = this.url;
			if(requestString == null) requestString = "/";
			out.write(requestString.getBytes());
			out.write(SUFFIX); 
		}
	} 
	
	public void writeTo(OutputStream out) throws IOException{
		writeHttpLine(out);
		out.write(CLCR);
		Iterator<Entry<String, String>> it = headers.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, String> kv = it.next();
			out.write(kv.getKey().getBytes());
			out.write(KV_SPLIT);
			out.write(kv.getValue().getBytes());
			out.write(CLCR);
		}
		out.write(CLCR);
		if(body != null){
			out.write(body);
		}
	}  
	
	public FileForm getFileForm() {
		return fileForm;
	}
	
	public static class FileUpload { 
		public String fileName;
		public String contentType;
		public byte[] data;
	}
	
	public static class FileForm {
		public Map<String, String> attributes = new HashMap<String, String>();
		public Map<String, List<FileUpload>> files = new HashMap<String, List<FileUpload>>();
	}
}