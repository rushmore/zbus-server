package io.zbus.auth;

import com.alibaba.fastjson.JSON;

import io.zbus.kit.JsonKit;
import io.zbus.transport.Message;

public class SignAuthExample {
	public static void main(String[] args) {
		ApiKeyProvider apiKeyProvider = new XmlApiKeyProvider("rpc/auth.xml");
		RequestAuth auth = new DefaultAuth(apiKeyProvider);
		
		RequestSign sign = new DefaultSign();
		
		Message req = new Message();
		req.setHeader("cmd", "pub"); 
		req.setHeader("mq", "MyRpc"); 
		req.setHeader("ack", false); 
		 
		req.setBody(new Object[] {1,2});
		
		String apiKey = "2ba912a8-4a8d-49d2-1a22-198fd285cb06";
		String secretKey = "461277322-943d-4b2f-b9b6-3f860d746ffd";
		
		sign.sign(req, apiKey, secretKey);
		
		String wired = JsonKit.toJSONString(req);
		System.out.println(wired);
		Message req2 = JsonKit.parseObject(wired, Message.class);
		AuthResult res = auth.auth(req2);
		
		System.out.println(res.success); 
		
		System.out.println(JSON.toJSONString(false));
	}
}
