package io.zbus.mq.api;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

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

}
