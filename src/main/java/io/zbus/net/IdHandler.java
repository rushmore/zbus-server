package io.zbus.net;

public interface IdHandler<REQ, RES>{ 
	void setRequestId(REQ message, String id);
	String getResponseId(RES message); 
}
