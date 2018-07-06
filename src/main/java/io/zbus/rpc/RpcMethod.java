package io.zbus.rpc;

import java.util.ArrayList;
import java.util.List;

import io.zbus.kit.HttpKit;
import io.zbus.rpc.annotation.RequestMapping;

public class RpcMethod {
	public String urlPath;  // java method's url path 
	public String method;   // java method
	public List<String> paramTypes = new ArrayList<>();
	public List<String> paramNames = new ArrayList<>();
	public String returnType; 
	public boolean authRequired;
	public boolean docEnabled = true;
	public RequestMapping urlAnnotation;
	
	public RpcMethod() {
		
	}
	
	public RpcMethod(RpcMethod m) { 
		this.method = m.method;
		this.paramTypes = new ArrayList<>(m.paramTypes);
		this.paramNames = new ArrayList<>(m.paramNames);
		this.returnType = m.returnType;
		this.authRequired = m.authRequired;
	} 
	
	public String getUrlPath() {
		if(urlPath == null) return HttpKit.joinPath(method);
		return urlPath;
	} 
	
	public void setUrlPath(String module, String method) {
		this.urlPath = HttpKit.joinPath(module, method);
	} 
}