package io.zbus.net.http;

import io.zbus.transport.Message;

public class HttpClientExample {
 
	public static void main(String[] args) throws Exception {  
		HttpClient client = new HttpClient();
		Message message = new Message();
		message.setUrl("https://test.io");
		String res = client.string(message);
		System.out.println(res); 
	} 
}
