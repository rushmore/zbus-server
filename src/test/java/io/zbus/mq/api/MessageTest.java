package io.zbus.mq.api;

import com.alibaba.fastjson.JSON;

public class MessageTest {

	public static void main(String[] args) {
		
		Message msg = new Message(); 
		msg.setTopic("fruit.*");
		
		msg.setCmd("produce");
		msg.setAppId("GuoSen");
		msg.setToken("x!$$#ds&!$d#!");
		msg.setHeader("example-key", "example-value");
		msg.setBody(new byte[1024]);
		 
		
		System.out.println(JSON.toJSONString(msg));
	} 
}
