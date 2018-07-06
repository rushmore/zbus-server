package io.zbus.rpc;

import java.util.HashMap;
import java.util.Map;

import io.zbus.kit.FileKit;
import io.zbus.rpc.annotation.RequestMapping;
import io.zbus.transport.Message;

public class RpcServerDirectHttp {   
	private FileKit fileKit = new FileKit(false); 
	
	@RequestMapping("/")
	public Message home() { 
		Message res = new Message();
		res.setStatus(200);
		res.setHeader("content-type", "text/html; charset=utf8"); 
		res.setBody("<h1>home page</h1>");
		
		return res;
	}
	
	@RequestMapping("/showUpload")
	public Message showUpload() { 
		return fileKit.loadResource("page/upload.html"); 
	}
	
	@RequestMapping("/upload")
	public Message doUpload(Message req) {  
		FileKit.saveUploadedFile(req, "/tmp/upload");
		Message res = new Message();
		
		res.setStatus(200);
		res.setHeader("content-type", "text/html; charset=utf8"); 
		res.setBody("<h1>Uploaded Success</h1>");
		
		return res;
	} 
	 
	@RequestMapping(path="/abc")
	public Object json() {
		Map<String, Object> value = new HashMap<>();
		value.put("key", System.currentTimeMillis());
		return value;
	}
	
	@RequestMapping(path="/favicon.ico", docEnabled=false)
	public Message favicon() { 
		return fileKit.loadResource("static/favicon.ico"); 
	}
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		
		RpcProcessor p = new RpcProcessor();
		StaticResource resource = new StaticResource();
		resource.setBasePath("\\tmp"); 
		 
		p.mount("", RpcServerDirectHttp.class); 
		p.mount("static", resource);
		
		RpcServer server = new RpcServer(p);       
		server.setPort(8080);
		server.start();
	}
}
