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
package io.zbus.net.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.net.Invoker;
import io.zbus.net.Client.MsgHandler;
import io.zbus.net.Sync.Id;
import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory; 

/**
 * HTTP Protocol Message, stands for both request and response formats.
 * 
 * Message is NOT standard HTTP protocol, but compatible to HTTP standard format.
 * Message may extend HTTP protocol in the header part, for example: mq: MyMQ\r\n
 * means a header extension of Key=mq, Value=MyMQ.
 * 
 * @author rushmore (洪磊明)
 *
 */
public class Message implements Id {  
	private static final Logger log = LoggerFactory.getLogger(Message.class); 
	private static final String DEFAULT_ENCODING = "UTF-8"; 
	
	public static final String HEARTBEAT        = "heartbeat"; 
	
	//使用到的标准HTTP头部
	public static final String REMOTE_ADDR      = "remote-addr";
	public static final String CONTENT_LENGTH   = "content-length";
	public static final String CONTENT_TYPE     = "content-type";
	 
	public static final String CMD    	= "cmd";     
	public static final String MQ       = "mq";
	public static final String SENDER   = "sender"; 
	public static final String RECVER   = "recver";
	public static final String ID      	= "id";	     
	
	public static final String SERVER   = "server"; 
	public static final String TOPIC    = "topic";  //use ',' to seperate different topics
	public static final String ACK      = "ack";	  
	public static final String ENCODING = "encoding";
	public static final String DELAY    = "delay";
	public static final String TTL      = "ttl";  
	
	public static final String ORIGIN_ID    = "rawid";      //original id
	public static final String ORIGIN_URL   = "origin_url"; //original URL  
	public static final String ORIGIN_STATUS= "reply_code"; //original Status 
	
	public static final String KEY       = "key";  
	public static final String KEY_GROUP = "key_group";
	
	//MQ copy, Master-Slave
	public static final String MASTER_MQ     = "master_mq";
	public static final String MASTER_TOKEN  = "master_token"; 
	
	 
	//1) First line of HTTP protocol
	private Meta meta = new Meta(); 
	//2) HTTP Key-Value headers
	private Map<String, String> head = new ConcurrentHashMap<String, String>();
	//HTTP body
	private byte[] body; 
	
	public Message(){
		setBody((byte[])null);
		setHead("connection", "Keep-Alive");  
	} 
	
	public Message(String body){
		setBody(body); 
		setHead("connection", "Keep-Alive");
	}
	
	public Message(byte[] body){
		setBody(body);
		setHead("connection", "Keep-Alive");
	}
	
	public static Message copyWithoutBody(Message msg){
		Message res = new Message();
		res.meta = new Meta(msg.meta);
		res.head = new ConcurrentHashMap<String, String>(msg.head);
		res.body = msg.body;
		return res;
	}
	
	/**
	 * HTTP request string
	 * eg. http://localhost/hello?xx=yy
	 * url=/hello?xx=yy
	 * @return
	 */
	public String getUrl(){
		return this.meta.url;
	} 
	public Message setUrl(String url){ 
		this.meta.setUrl(url);
		this.meta.status = null; //once requestString is set, it becomes a request message
		return this;
	}
	
	public Message setStatus(String status) { 
		meta.status = status;
		return this; 
	} 
	
	public String getStatus(){
		return meta.status;
	}
	
	public Message setStatus(int status){
		return setStatus(""+status);
	}
	
	public boolean isRequest(){
		return this.getStatus() == null;
	}
	
	public boolean isResponse(){
		return this.getStatus() != null;
	}
	
	public Message asResponse(){
		return setStatus(200);
	}
	
	public Message asResponse(String status){
		return setStatus(status);
	}
	
	public Message asResponse(int status){
		return setStatus(status);
	}
	
	public Message asRequest(String url){
		return setUrl(url);
	}
	
	public Message asRequest(){
		return setUrl("/");
	}
	
	public String getMethod(){
		return meta.getMethod();
	}
	
	public void setMethod(String method){
		this.meta.setMethod(method);
	}
	
	/**
	 * HTTP请求串
	 * eg. http://localhost/hello?xx=yy
	 * requestPath=/hello
	 * @return
	 */
	public String getRequestPath(){
		return this.meta.requestPath;
	}
	 
	public void setRequestPath(String path){
		meta.setRequestPath(path);
	}
	
	public Map<String, String> getRequestParams(){
		return meta.requestParams;
	}
	public String getRequestParam(String key){  
		String value = meta.getRequestParam(key); 
		if(value == null) return null;
		return urlDecode(value);
	}  
	public void setRequestParam(String key, String value){ 
		value = urlEncode(value);
		meta.setRequestParam(key, value);
	}
	
	public Map<String,String> getHead() {
		return head;
	} 
	
	public void setHead(Map<String,String> head) {
		this.head = head;
	} 
	
	public String getHead(String key){
		return this.head.get(key);
	}
	
	public String getHead(String key, String defaultValue){
		String res = this.head.get(key);
		if(res == null) return defaultValue;
		return res;
	}
	
	public void setHead(String key, String value){
		if(value == null) return;
		this.head.put(key, value);
	} 
	
	public void setHead(String key, Object value){
		if(value == null) return;
		this.head.put(key, value.toString());
	} 
	
	public String removeHead(String key){
		return this.head.remove(key);
	}
	
	public byte[] getBody() {
		byte[] b = body;
		String bodyOfHead = getHead("body");
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
		this.setHead(CONTENT_LENGTH, ""+len);
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
	
	public Message setBody(File file) throws IOException{
		InputStream in = null;
		try{
			in = new FileInputStream(file);
			if(in.available() > 0){
				this.body = new byte[in.available()];
				in.read(this.body);
			} 
		}finally{
			if(in != null){
				in.close();
			}
		}
		return this;
	}
	
	public void getBodyAsFile(File file) throws IOException{
		if(this.body == null) return;
		OutputStream out = null;
		try{
			out = new FileOutputStream(file);
			out.write(this.body);
		} finally{
			if(out != null){
				out.close();
			}
		} 
	}
	
	public Message setJsonBody(String body){
		return this.setJsonBody(body.getBytes());
	}
	
	public Message setJsonBody(byte[] body){
		this.setHead(CONTENT_TYPE, "application/json");
		this.setBody(body);
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
	
	//////////////////////////////////////////////////////////////
	public void decodeHeaders(InputStream in){ 
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		try{  
			inputStreamReader = new InputStreamReader(in);
			bufferedReader = new BufferedReader(inputStreamReader);
			String meta = bufferedReader.readLine();
			if(meta == null) return;
			this.meta = new Meta(meta);
			
			String line = bufferedReader.readLine();
	        while (line != null && line.trim().length() > 0) {
	            int p = line.indexOf(':');
	            if (p >= 0){ 
	                head.put(line.substring(0, p).trim().toLowerCase(), line.substring(p + 1).trim());
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
	
	public String getCmd() { 
		return this.getHead(CMD);
	} 
	public Message setCmd(String value) {
		this.setHead(CMD, value); 
		return this;
	}   
	
	public String getServer(){
		return this.getHead(SERVER);
	}  
	public void setServer(String value){
		this.setHead(SERVER, value);
	} 
	
	public String getSender() {
		return this.getHead(SENDER);
	} 
	public Message setSender(String value) {
		this.setHead(SENDER, value);
		return this;
	}
	
	
	public String getRecver() {
		return this.getHead(RECVER);
	} 
	public Message setRecver(String value) {
		this.setHead(RECVER, value);
		return this;
	}
	
	
	public String getRemoteAddr() {
		return this.getHead(REMOTE_ADDR);
	} 
	public Message setRemoteAddr(String value) {
		this.setHead(REMOTE_ADDR, value);
		return this;
	}  
	
	
	public String getOriginStatus() {
		return this.getHead(ORIGIN_STATUS);
	} 
	public Message setOriginStatus(String value) {
		this.setHead(ORIGIN_STATUS, value);
		return this;
	}  
	
	public String getOriginUrl() {
		return this.getHead(ORIGIN_URL);
	} 
	public Message setOriginUrl(String value) {
		this.setHead(ORIGIN_URL, value);
		return this;
	}   
	
	public String getOriginId() {
		return this.getHead(ORIGIN_ID);
	} 
	public Message setOriginId(String value) {
		if(value == null) return this;
		this.setHead(ORIGIN_ID, value);
		return this;
	}
	 
	
	public String getEncoding() {
		return this.getHead(ENCODING);
	} 
	public Message setEncoding(String encoding) {
		this.setHead(ENCODING, encoding);
		return this;
	}
	
	public String getDelay() {
		return this.getHead(DELAY);
	} 
	public Message setDelay(String value) {
		this.setHead(DELAY, value);
		return this;
	} 
	
	public String getTtl() {
		return this.getHead(TTL);
	} 
	public Message setTtl(String value) {
		this.setHead(TTL, value);
		return this;
	} 
	
	public String getId() {
		return this.getHead(ID);
	} 
	public void setId(String msgId) {
		if(msgId == null) return;
		this.setHead(ID, msgId); 
	}	
	public void setId(long id){
		setId(""+id);
	} 
	
	public boolean isAck() {
		String ack = this.getHead(ACK);
		if(ack == null) return true; //default to true
		ack = ack.trim().toLowerCase();
		return ack.equals("1") || ack.equals("true");
	} 
	public void setAck(boolean ack){
		String value = ack? "1":"0";
		this.setHead(ACK, value);
	}
	
	
	public String getMq(){
		String value = this.getHead(MQ);
		return value;
	} 
	public Message setMq(String mq) {
		this.setHead(MQ, mq);
		return this;
	} 
	
	
	public String getTopic() {
		return getHead(TOPIC);
	} 
	public Message setTopic(String topic) {
		this.setHead(TOPIC, topic);
		return this;
	}  
	
	public String getKey() {
		return getHead(KEY);
	} 
	public Message setKey(String value) {
		this.setHead(KEY, value);
		return this;
	} 
	public String getKeyGroup() {
		return getHead(KEY_GROUP);
	} 
	public Message setKeyGroup(String value) {
		this.setHead(KEY_GROUP, value);
		return this;
	} 
	
	public String getMasterMq() {
		return getHead(MASTER_MQ);
	} 
	public Message setMasterMq(String value) {
		this.setHead(MASTER_MQ, value);
		return this;
	} 
	
	public String getMasterToken() {
		return getHead(MASTER_TOKEN);
	} 
	public Message setMasterToken(String value) {
		this.setHead(MASTER_TOKEN, value);
		return this;
	} 
	
	public boolean isStatus200() {
		return "200".equals(this.getStatus());
	} 
	public boolean isStatus404() {
		return "404".equals(this.getStatus());
	} 
	public boolean isStatus500() {
		return "500".equals(this.getStatus());
	}  
	public boolean isStatus406() {
		return "406".equals(this.getStatus());
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
		sb.append(meta+"\r\n");
		
		List<String> keys = new ArrayList<String>(head.keySet());
		Collections.sort(keys);
		
		for(String key : keys){ 
			String val = head.get(key);
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
		String contentLength = msg.getHead(Message.CONTENT_LENGTH);
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
	
	private void writeTo(OutputStream out) throws IOException{
		meta.writeTo(out);
		out.write(CLCR);
		Iterator<Entry<String, String>> it = head.entrySet().iterator();
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
	
	static private String urlDecode(String str) {
        String decoded = null;
        try {
            decoded = URLDecoder.decode(str, "UTF8");
        } catch (UnsupportedEncodingException ignored) {
        }
        return decoded;
    }
	
	static private String urlEncode(String str) {
        String encoded = null;
        try {
        	encoded = URLEncoder.encode(str, "UTF8");
        } catch (UnsupportedEncodingException ignored) {
        }
        return encoded;
    }

	static class Meta{   
		String status; //null if request type, HTTP status code if response	
		//HTTP request method
		String method = "GET"; 
		
		String url = "/";         //request string(updated by requestPath and requestParams)
		
		String requestPath = url; //request path
		Map<String,String> requestParams;   //request key-value pairs 
		
		static Set<String> httpMethod = new HashSet<String>();
		static Map<String,String> httpStatus = new HashMap<String, String>();
		
		static{ 
			httpMethod.add("GET");
			httpMethod.add("POST"); 
			httpMethod.add("PUT");
			httpMethod.add("DELETE");
			httpMethod.add("HEAD");
			httpMethod.add("OPTIONS"); 
			
			httpStatus.put("101", "Switching Protocols"); 
			httpStatus.put("200", "OK");
			httpStatus.put("201", "Created");
			httpStatus.put("202", "Accepted");
			httpStatus.put("204", "No Content"); 
			httpStatus.put("206", "Partial Content"); 
			httpStatus.put("301", "Moved Permanently");
			httpStatus.put("304", "Not Modified"); 
			httpStatus.put("400", "Bad Request"); 
			httpStatus.put("401", "Unauthorized"); 
			httpStatus.put("403", "Forbidden");
			httpStatus.put("404", "Not Found"); 
			httpStatus.put("405", "Method Not Allowed"); 
			httpStatus.put("416", "Requested Range Not Satisfiable");
			httpStatus.put("500", "Internal Server Error");
		} 
		
		@Override
		public String toString() { 
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				writeTo(out);
				return new String(out.toByteArray());
			} catch (IOException e) {
				//ignore
				return null;
			} 
		} 
		
		public void writeTo(OutputStream out) throws IOException{
			if(this.status != null){
				String desc = httpStatus.get(this.status);
				if(desc == null){
					desc = "Unknown Status";
				}
				out.write(PREFIX);
				out.write(status.getBytes());
				out.write(BLANK);
				out.write(desc.getBytes());  
			} else {
				String method = this.method; 
				if(method == null) method = ""; 
				out.write(method.getBytes());
				out.write(BLANK); 
				String requestString = this.url;
				if(requestString == null) requestString = "";
				out.write(requestString.getBytes());
				out.write(SUFFIX); 
			}
		} 
		final static byte[] BLANK = " ".getBytes();
		final static byte[] PREFIX = "HTTP/1.1 ".getBytes();
		final static byte[] SUFFIX = " HTTP/1.1".getBytes(); 
		 
		public Meta(){ }
		
		public Meta(Meta m){
			this.url = m.url;
			this.requestPath = m.requestPath;
			this.method = m.method;
			this.status = m.status;
			if(m.requestParams != null){
				this.requestParams = new HashMap<String, String>(m.requestParams);
			}
		}
		
		public Meta(String meta){
			if("".equals(meta)){
				return;
			}
			StringTokenizer st = new StringTokenizer(meta);
			String firstWord = st.nextToken();
			if(firstWord.toUpperCase().startsWith("HTTP")){ //As response
				this.status = st.nextToken();
				return;
			}
			//As request
			this.method = firstWord;  
			this.url = st.nextToken();
			decodeUrl(this.url);
		} 
		
		public String getMethod(){
			return this.method;
		}
		
		public void setMethod(String method){
			this.method = method;
		}
		
		public void setUrl(String url){
			this.url = url;
			decodeUrl(url);
		} 
		
		private void calcUrl(){
			StringBuilder sb = new StringBuilder();
			if(this.requestPath != null){ 
				sb.append(this.requestPath);
			}
			if(this.requestParams != null){
				sb.append("?");
				Iterator<Entry<String, String>> it = requestParams.entrySet().iterator();
				while(it.hasNext()){
					Entry<String, String> e = it.next();
					sb.append(e.getKey()+"="+e.getValue());
					if(it.hasNext()){
						sb.append("&&");
					}
				}
			}
			this.url = sb.toString(); 
		}
		private void decodeUrl(String url){
			int idx = url.indexOf('?');
			if(idx < 0){
				this.requestPath = urlDecode(url);
			} else {
				this.requestPath = url.substring(0, idx);
			}  
			if(!this.requestPath.equals("/")){
				if(this.requestPath.endsWith("/")){
					this.requestPath = this.requestPath.substring(0, this.requestPath.length()-1);
				}
			}
			if(idx < 0) return;
			if(this.requestParams == null){
				this.requestParams = new ConcurrentHashMap<String, String>(); 
			}
			String paramString = url.substring(idx+1); 
			StringTokenizer st = new StringTokenizer(paramString, "&");
	        while (st.hasMoreTokens()) {
	            String e = st.nextToken();
	            int sep = e.indexOf('=');
	            if (sep >= 0) {
	                this.requestParams.put(urlDecode(e.substring(0, sep)).trim(),
	                		urlDecode(e.substring(sep + 1)));
	            } else {
	                this.requestParams.put(urlDecode(e).trim(), "");
	            }
	        } 
		}  
		
		public String getRequestParam(String key){
			if(requestParams == null) return null;
			return requestParams.get(key);
		}
		
		public String getRequestParam(String key, String defaultValue){
			String value = getRequestParam(key);
			if(value == null){
				value = defaultValue;
			}
			return value;
		}
		
		public void setRequestPath(String path){
			this.requestPath = path;
			calcUrl();
		}
		
		public void setRequestParam(String key, String value){
			if(requestParams == null){
				requestParams = new HashMap<String, String>();
			}
			requestParams.put(key, value);
			calcUrl();
		}
	}

	public static interface MessageHandler extends MsgHandler<Message> { }
	public static interface MessageInvoker extends Invoker<Message, Message> { }	
	public static interface MessageProcessor { 
		Message process(Message request);
	}
	
	
	public Message invokeSimpleHttp() throws IOException {
		String server = this.getServer();
		if(server == null){
			throw new IllegalArgumentException("request missing target server");
		}
		String format = "http://%s%s";
		if(server.startsWith("http://")){
			format = "%s%s";
		}
		String urlString = String.format(format, server, this.getUrl());
		URL url = new URL(urlString);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		for(Entry<String, String> e : this.getHead().entrySet()){
			conn.setRequestProperty(e.getKey(), e.getValue());
		}
		conn.setRequestMethod(this.getMethod());
		
		if(this.getBody() != null && this.getBody().length > 0){
			conn.setDoOutput(true);
			conn.getOutputStream().write(this.getBody());
		}
		
		int code = conn.getResponseCode();
		Message res = new Message();
		res.setStatus(code);
		
		for (Entry<String, List<String>> header : conn.getHeaderFields().entrySet()){
			String key = header.getKey();
			if(key == null) continue;
			key = key.toLowerCase();
			if(key.equals("transfer-encoding")){ //ignore transfer-encoding
				continue;
			}
			
			List<String> values = header.getValue();
			String value = null;
			if(values.size() == 1){
				value = values.get(0);
			} else if(values.size() > 1){
				value = "[";
				for(int i=0; i<values.size(); i++){
					if(i == value.length()-1){
						value += values.get(i);
					} else {
						value += values.get(i) + ",";
					}
				}
			} 
			res.setHead(key, value);
		}
		InputStream bodyStream = conn.getInputStream();
		if(bodyStream != null){
			ByteArrayOutputStream sink = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			int len = 0;
			while((len=bodyStream.read(buf))>=0){
				sink.write(buf, 0, len);
			}
			res.setBody(sink.toByteArray()); 
		} 
		conn.disconnect(); 
		return res;
	}
	
}