package io.zbus.rpc;

import java.util.Arrays;

public class Request{ 
	private String module = "";  
	private String method;       
	private Object[] params;     
	private String[] paramTypes; 
	
	public String getModule() {
		return module;
	}
	public void setModule(String module) {
		this.module = module;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public Object[] getParams() {
		return params;
	}
	public void setParams(Object[] params) {
		this.params = params;
	}
	public String[] getParamTypes() {
		return paramTypes;
	}
	public void setParamTypes(String[] paramTypes) {
		this.paramTypes = paramTypes;
	} 
	public Request method(String method){
		this.method = method;
		return this;
	}
	public Request module(String module){
		this.module = module;
		return this;
	}
	public Request params(Object... params){
		this.params = params;
		return this;
	} 
	
	public Request paramTypes(Class<?>... types){
		if(types == null) return this;
		this.paramTypes = new String[types.length];
		for(int i=0; i<types.length; i++){
			this.paramTypes[i]= types[i].getCanonicalName(); 
		}
		return this;
	} 
	
	public static void normalize(Request req){
		if(req.module == null){
			req.module = "";
		}
		if(req.params == null){
			req.params = new Object[0];
		}
	}
	@Override
	public String toString() {
		return "Request [module=" + module + ", method=" + method + ", params=" + Arrays.toString(params)
				+ ", paramTypes=" + Arrays.toString(paramTypes) + "]";
	}   
}