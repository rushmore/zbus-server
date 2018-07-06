package io.zbus.transport.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.zbus.kit.JsonKit;
import io.zbus.transport.Message;

/**
 * 
 * HTTP conversion for <code>Message</code>.
 * 
 * @author leiming.hong Jun 27, 2018
 *
 */
public class Http {
	private static final Logger logger = LoggerFactory.getLogger(Http.class);   
	public static final String URL     = "url";
	public static final String STATUS  = "status";
	public static final String BODY    = "body";
	public static final String HEADERS = "headers";
	
	public static final String CONTENT_LENGTH        = "content-length";
	public static final String CONTENT_TYPE          = "content-type";     
	public static final String CONTENT_TYPE_JSON     = "application/json; charset=utf8"; 
	public static final String CONTENT_TYPE_UPLOAD   = "multipart/form-data";
	
	private static final byte[] CLCR = "\r\n".getBytes();
	private static final byte[] KV_SPLIT = ": ".getBytes();
	private static final byte[] BLANK = " ".getBytes();
	private static final byte[] PREFIX = "HTTP/1.1 ".getBytes();
	private static final byte[] SUFFIX = " HTTP/1.1".getBytes();  
	
	public static byte[] toBytes(Message msg){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			writeTo(msg, out);
		} catch (IOException e) {
			return null;
		}
		return out.toByteArray(); 
	}  
	
	public static String toString(Message msg){
		return new String(toBytes(msg)); //TODO encoding
	}  
	
	public static byte[] body(Message msg) {
		String contentType = msg.getHeader(CONTENT_TYPE);
		byte[] body = new byte[0];
		Object bodyObj = msg.getBody();
		String charset = "utf8";
		if(contentType != null) { 
			charset = charset(contentType);
		}  
		
		if(contentType != null && contentType.startsWith("application/json")) {
			body = JsonKit.toJSONBytes(msg.getBody(), charset);
		}  else {
			if(bodyObj != null) {
				if(bodyObj instanceof byte[]) { 
					body = (byte[])bodyObj;
				} else if(bodyObj instanceof String) { 
					body = msg.getBody().toString().getBytes();
				} else { 
					body = JsonKit.toJSONBytes(msg.getBody(), charset);
				}
			}   
		} 
		return body;
	}
	
	public static Message parse(byte[] data){
		int idx = findHeaderEnd(data);
		if(idx == -1){
			throw new IllegalArgumentException("Invalid input byte array");
		}
		int headLen = idx + 1;
		Message msg = new Message();
		decodeHeaders(msg, data, 0, headLen);
		String contentLength = msg.getHeader(CONTENT_LENGTH);
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
	
	public static String charset(String contentType) {
		String charset="utf-8";
		String[] bb = contentType.split(";"); 
		if(bb.length>1){
			String[] bb2 = bb[1].trim().split("=");
			if(bb2[0].trim().equalsIgnoreCase("charset")){
				charset = bb2[1].trim();
			} 
		} 
		return charset;
	}
	
	private static void writeTo(Message msg, OutputStream out) throws IOException{
		writeHttpLine(msg,out);
		out.write(CLCR);
		Map<String, String> headers = new HashMap<>(msg.getHeaders()); 
		String contentType = headers.get(CONTENT_TYPE);
		if(contentType == null) { 
			contentType = "application/json; charset=utf8";
			headers.put(CONTENT_TYPE, contentType); 
		}
		byte[] body = body(msg); 
		headers.put(CONTENT_LENGTH, body.length+"");
		for(Entry<String, String> e : headers.entrySet()) { 
			out.write(e.getKey().getBytes());
			out.write(KV_SPLIT);
			out.write(e.getValue().getBytes());
			out.write(CLCR);
		} 
		out.write(CLCR);
		if(body != null){
			out.write(body);
		}
	}     
	
	
	private static void decodeHeaders(Message msg, byte[] data, int offset, int size){
		ByteArrayInputStream in = new ByteArrayInputStream(data, offset, size);
		decodeHeaders(msg, in);
		if(in != null){
			try { in.close(); } catch (IOException e) {}
		} 
	}   
	
	private static void decodeHeaders(Message msg, InputStream in){ 
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
				msg.setStatus(Integer.valueOf(st.nextToken())); 
			} else {
				msg.setMethod(start);  
				msg.setUrl(st.nextToken());
			}
			
			String line = bufferedReader.readLine();
	        while (line != null && line.trim().length() > 0) {
	            int p = line.indexOf(':');
	            if (p >= 0){ 
	                msg.setHeader(line.substring(0, p).trim().toLowerCase(), line.substring(p + 1).trim());
	            } 
	            line = bufferedReader.readLine();
	        }
	       
		} catch(IOException e){ 
			logger.error(e.getMessage(), e);
		} finally {
			if(bufferedReader != null){
				try { bufferedReader.close(); } catch (IOException e) {}
			}
			if(inputStreamReader != null){
				try { inputStreamReader.close(); } catch (IOException e) {}
			} 
		}
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
	 
	
	private static void writeHttpLine(Message msg, OutputStream out) throws IOException{
		if(msg.getStatus() != null){
			String statusText = msg.getStatusText();
			if(statusText == null) {
				HttpResponseStatus s = HttpResponseStatus.valueOf(msg.getStatus());
				if(s != null){
					statusText = s.reasonPhrase();
				} else {
					statusText = "Unknown Status";
				}
			}
			out.write(PREFIX);
			out.write(String.format("%d", msg.getStatus()).getBytes());
			out.write(BLANK);
			out.write(statusText.getBytes());  
		} else {
			String method = msg.getMethod(); 
			if(method == null) method = "GET"; 
			out.write(method.getBytes());
			out.write(BLANK); 
			String requestString = msg.getUrl();
			if(requestString == null) requestString = "/";
			out.write(requestString.getBytes());
			out.write(SUFFIX); 
		}
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
