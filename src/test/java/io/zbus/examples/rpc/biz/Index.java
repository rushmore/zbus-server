package io.zbus.examples.rpc.biz;

import java.io.IOException;

import io.zbus.kit.FileKit;
import io.zbus.kit.HttpKit;
import io.zbus.transport.http.Message;
 
public class Index { 
	
	public Message index(Message request) {
		String url = request.getUrl(); // /static/resource/app.js 
		boolean hasTopic = request.getHeader("topic") != null;
		String resource = HttpKit.rpcUrl(url, hasTopic);
		
		Message res = new Message();
		res.setStatus(200);
		try {
			byte[] data = FileKit.loadFileBytes(resource);
			res.setBody(data); 
			String contentType = HttpKit.contentType(resource);
			if(contentType == null){
				contentType = "text/plain";
			}
			res.setHeader("content-type", contentType); 
		} catch (IOException e) {
			res.setStatus(404);
			res.setBody(e.getMessage());
		}
		return res;
	} 
} 

 