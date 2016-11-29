package io.zbus.mq.api;
 

public class MessageTest {

	public static void main(String[] args) {
		
		Message msg = new Message() 
			.cmd("produce")
			.topic("MyTopic")
			.appId("GuoSen")
			.token("x!$$#ds&!$d#!")
			.header("example-key", "example-value")
			.body(new byte[1024]);
		 
		
		System.out.println(msg);
	} 
}
