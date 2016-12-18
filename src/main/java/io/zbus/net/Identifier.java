package io.zbus.net;

public interface Identifier<REQ, RES>{ 
	void setRequestId(REQ request, String id);
	String getResponseId(RES response); 
}
