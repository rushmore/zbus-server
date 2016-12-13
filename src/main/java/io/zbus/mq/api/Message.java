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
	public static final String CMD         = "Cmd";
	public static final String TOPIC       = "Topic";
	public static final String GROUP       = "Group";
	public static final String APPID       = "AppId";
	public static final String TOKEN       = "Token";
	public static final String ACK         = "Ack";
	public static final String TAG         = "Tag";
	public static final String ID          = "Id";
	public static final String SENDER      = "Sender";
	public static final String RECEIVER    = "Receiver";
	public static final String WINDOW      = "Window";
	public static final String BATCH_SIZE  = "BatchSize";
	public static final String BATCH_IN_TX = "BatchInTx";

	private Integer status;
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

	public String getGroup() {
		return getHeader(GROUP);
	}

	public void setGroup(String value) {
		setHeader(GROUP, value);
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

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer value) {
		this.status = value; 
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
}
