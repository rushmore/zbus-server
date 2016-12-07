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
	public static final String Cmd       = "Cmd";
	public static final String Topic     = "Topic";
	public static final String Group     = "Group";
	public static final String AppId     = "AppId";
	public static final String Token     = "Token";
	public static final String Ack       = "Ack";
	public static final String Tag       = "Tag";
	public static final String Id        = "Id";
	public static final String Sender    = "Sender";
	public static final String Receiver  = "Receiver";
	public static final String Window    = "Window";
	public static final String BatchSize = "BatchSize";
	public static final String BatchInTx = "BatchInTx";

	private Integer status;
	private Map<String, String> headers = new HashMap<String, String>();
	private byte[] body;

	public void setCmd(String value) {
		setHeader(Cmd, value);
	}

	public String getCmd() {
		return getHeader(Cmd);
	}

	public String getTopic() {
		return getHeader(Topic);
	}
	
	public void setTopic(String value) {
		setHeader(Topic, value);
	} 

	public String getGroup() {
		return getHeader(Group);
	}

	public void setGroup(String value) {
		setHeader(Group, value);
	}

	public String getAppId() {
		return getHeader(AppId);
	}

	public void setAppId(String value) {
		setHeader(AppId, value);
	}

	public String getToken() {
		return getHeader(Token);
	}

	public void setToken(String value) {
		setHeader(Token, value);
	}

	public boolean getAck() {
		String value = getHeader(Ack);
		return value == null ? false : Boolean.valueOf(value);
	}

	public void setAck(boolean value) {
        setHeader(Ack, String.valueOf(value));
	}

	public String getId() {
		return getHeader(Id);
	}

	public void setId(String value) {
		setHeader(Id, value);
	}

	public String getsender() {
		return getHeader(Sender);
	}

	public void setSender(String value) {
	    setHeader(Sender, value);
	}

	public String getReceiver() {
		return getHeader(Receiver);
	}

	public void setReceiver(String value) {
		setHeader(Receiver, value);
	}

	public Integer getWindow() {
		String value = getHeader(Window);
		return value == null ? null : Integer.valueOf(value);
	}

	public void setWindow(Integer value) {
		setHeader(Window, String.valueOf(value));
	}

	public Integer getBatchSize() {
		String value = getHeader(BatchSize);
		return value == null ? null : Integer.valueOf(value);
	}

	public void setBatchSize(Integer value) {
		setHeader(BatchSize, String.valueOf(value));
	}

	public boolean getBatchInTx() {
		String value = getHeader(BatchInTx);
		return value == null ? false : Boolean.valueOf(value);
	}

	public void setBatchInTx(boolean value) {
		setHeader(BatchInTx, String.valueOf(value));
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
