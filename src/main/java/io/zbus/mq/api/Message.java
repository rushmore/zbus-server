package io.zbus.mq.api;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * 
 * Status[null], null usually interpreted as request 
 * Header(Key-Value pairs)
 * Body
 * 
 * Message is designed to be easy to serialize as HTTP format.
 * 
 * @author Rushmore
 *
 */
public class Message {
	public static final String CMD         = "cmd"; 
	public static final String TOPIC       = "topic";
	public static final String CHANNEL     = "channel";
	public static final String APPID       = "appid";
	public static final String TOKEN       = "token";
	public static final String ACK         = "ack";
	public static final String TAG         = "tag";
	public static final String ID          = "id";
	public static final String SENDER      = "sender";
	public static final String RECEIVER    = "receiver";
	public static final String WINDOW      = "window";
	public static final String BATCH_SIZE  = "batch-size";
	public static final String BATCH_IN_TX = "batch-in-tx"; 

	private Integer status; //decide whether message is request or response
	private String method;  //default GET
	private String url;     //default /
	
	private Map<String, String> headers = new HashMap<String, String>();
	private byte[] body;

	public void setCmd(String value) {
		setHeader(CMD, value);
	}

	public String getCmd() {
		return getHeader(CMD);
	} 

	public String getTopic() {
		return getHeader(TOPIC);
	}
	
	public void setTopic(String value) {
		setHeader(TOPIC, value);
	} 

	public String getChannel() {
		return getHeader(CHANNEL);
	}

	public void setChannel(String value) {
		setHeader(CHANNEL, value);
	}

	public String getAppId() {
		return getHeader(APPID);
	}

	public void setAppId(String value) {
		setHeader(APPID, value);
	}

	public String getToken() {
		return getHeader(TOKEN);
	}

	public void setToken(String value) {
		setHeader(TOKEN, value);
	}

	public boolean getAck() {
		String value = getHeader(ACK);
		return value == null ? false : Boolean.valueOf(value);
	}

	public void setAck(boolean value) {
        setHeader(ACK, String.valueOf(value));
	}

	public String getId() {
		return getHeader(ID);
	}

	public void setId(String value) {
		setHeader(ID, value);
	}

	public String getsender() {
		return getHeader(SENDER);
	}

	public void setSender(String value) {
	    setHeader(SENDER, value);
	}

	public String getReceiver() {
		return getHeader(RECEIVER);
	}

	public void setReceiver(String value) {
		setHeader(RECEIVER, value);
	}

	public Integer getWindow() {
		String value = getHeader(WINDOW);
		return value == null ? null : Integer.valueOf(value);
	}

	public void setWindow(Integer value) {
		setHeader(WINDOW, String.valueOf(value));
	}

	public Integer getBatchSize() {
		String value = getHeader(BATCH_SIZE);
		return value == null ? null : Integer.valueOf(value);
	}

	public void setBatchSize(Integer value) {
		setHeader(BATCH_SIZE, String.valueOf(value));
	}

	public boolean getBatchInTx() {
		String value = getHeader(BATCH_IN_TX);
		return value == null ? false : Boolean.valueOf(value);
	}

	public void setBatchInTx(boolean value) {
		setHeader(BATCH_IN_TX, String.valueOf(value));
	}

	public String getHeader(String key) {
		if (!headers.containsKey(key))
			return null;
		return headers.get(key);
	}

	public void setHeader(String key, String value) {
		if(key == null){
			throw new IllegalArgumentException("key must not be null");
		} 
		if(value == null) return;
		headers.put(key, value); 
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public byte[] getBody() {
		return body;
	}

	public Message setBody(byte[] value) {
		this.body = value; 
		return this;
	} 
	
	public Message setJsonBody(byte[] value){
		setBody(value);
		setHeader("content-type", "application/json");
		return this;
	}
	
	public void setBody(String value) {
		setBody(value.getBytes());
	}

	public void setBody(String value, Charset charset) {
		setBody(value.getBytes(charset));
	} 
	
	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer value) {
		this.status = value; 
	} 
	
	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public String toString() {
		OutputStream out = new ByteArrayOutputStream();
		try {
			writeTo(out);
			return out.toString();
		} catch (IOException e) {
			//ignore
		}
		return null;
	}  

	public void writeTo(OutputStream out) throws IOException{  
		if(this.status != null){   
			out.write(HTTP_PREFIX);
			out.write(String.format("%d", status).getBytes());
			out.write(HTTP_BLANK);
			out.write(HttpResponseStatus.valueOf(status).reasonPhrase().getBytes());  
		} else {
			String method = this.method; 
			if(method == null) method = "GET"; 
			out.write(method.getBytes());
			out.write(HTTP_BLANK); 
			String url = this.url;
			if(url == null) url = "/";
			out.write(url.getBytes());
			out.write(HTTP_SUFFIX); 
		}
		out.write(HTTP_CLCR);
		Iterator<Entry<String, String>> it = headers.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, String> kv = it.next();
			out.write(kv.getKey().getBytes());
			out.write(HTTP_KV_SPLIT);
			out.write(kv.getValue().getBytes());
			out.write(HTTP_CLCR);
		}
		out.write(HTTP_CLCR);
		if(body != null){
			out.write(body);
		}  
	}
	
	public void readFrom(byte[] data){
		int idx = findHeaderEnd(data);
		if(idx == -1){ 
			throw new IllegalArgumentException("Invalid input byte array");
		}
		int headLen = idx + 1;
		Message msg = new Message();
		ByteArrayInputStream in = new ByteArrayInputStream(data, 0, headLen);
		
		try {
			decodeHeaders(in);
		} catch (IOException e) {
			//ignore
			return;
		} 
		String contentLength = msg.getHeader(HTTP_LENGTH);
		if(contentLength == null){ //just head 
			return;
		}
		
		int bodyLen = Integer.valueOf(contentLength);   
		if(data.length != headLen + bodyLen) {
			throw new IllegalArgumentException("Invalid input byte array");
		}
		byte[] body = new byte[bodyLen];
		System.arraycopy(data, headLen, body, 0, bodyLen);
		msg.setBody(body);  
	}
	
	private void decodeMeta(String meta){
		if("".equals(meta)){
			return;
		}
		StringTokenizer st = new StringTokenizer(meta);
		String firstWord = st.nextToken();
		if(firstWord.toUpperCase().startsWith("HTTP")){ //As response
			status = Integer.valueOf(st.nextToken());
			return;
		}
		//As request
		method = firstWord;  
		url = st.nextToken(); 
	} 
	
	private void decodeHeaders(InputStream in) throws IOException{ 
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		try{  
			inputStreamReader = new InputStreamReader(in);
			bufferedReader = new BufferedReader(inputStreamReader);
			String meta = bufferedReader.readLine();
			if(meta == null){
				throw new IllegalStateException("Missing first line of HTTP package");
			}
			decodeMeta(meta);
			
			String line = bufferedReader.readLine();
	        while (line != null && line.trim().length() > 0) {
	            int p = line.indexOf(':');
	            if (p >= 0){ 
	            	setHeader(line.substring(0, p).trim().toLowerCase(), line.substring(p + 1).trim());
	            } 
	            line = bufferedReader.readLine();
	        } 
		} finally {
			if(bufferedReader != null){
				try { bufferedReader.close(); } catch (IOException e) {}
			}
			if(inputStreamReader != null){
				try { inputStreamReader.close(); } catch (IOException e) {}
			} 
		}
	}
	
	private int findHeaderEnd(byte[] data){ 
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
	private static final byte[] HTTP_CLCR = "\r\n".getBytes();
	private static final byte[] HTTP_KV_SPLIT = ": ".getBytes();
	private static final byte[] HTTP_BLANK = " ".getBytes();
	private static final byte[] HTTP_PREFIX = "HTTP/1.1 ".getBytes();
	private static final byte[] HTTP_SUFFIX = " HTTP/1.1".getBytes(); 
	private static final String HTTP_LENGTH = "content-length";
}
