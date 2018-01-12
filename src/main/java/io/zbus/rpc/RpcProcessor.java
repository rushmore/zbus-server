package io.zbus.rpc;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.zbus.kit.FileKit;
import io.zbus.kit.HttpKit;
import io.zbus.kit.StrKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.transport.http.Message; 


public class RpcProcessor {
	private static final Logger log = LoggerFactory.getLogger(RpcProcessor.class);  
	
	private RpcCodec codec = new JsonRpcCodec();
	private Map<String, MethodInstance> methods = new HashMap<String, MethodInstance>(); 
	private Map<String, List<RpcMethod>> object2Methods = new HashMap<String, List<RpcMethod>>();	
	
	private String docUrlContext = "/";
	private boolean enableStackTrace = true;
	private boolean enableMethodPage = true;
	
	public RpcProcessor(){
		addModule("index", new DocRender());
	}
	
	public void addModule(Object... services){
		for(Object obj : services){
			if(obj == null) continue;
			for(Class<?> intf : getAllInterfaces(obj.getClass())){
				addModule(intf.getSimpleName(), obj);
				addModule(intf.getName(), obj);
			} 
			addModule(obj.getClass().getSimpleName(), obj);
			addModule(obj.getClass().getName(), obj);
		} 
	}
	
	public void addModule(String module, Object... services){
		for(Object service: services){
			this.initCommandTable(module, service);
		}
	} 
	
	public void addModule(Class<?>... clazz){
		Object[] services = new Object[clazz.length];
		for(int i=0;i<clazz.length;i++){
			Class<?> c = clazz[i];
			try {
				services[i] = c.newInstance();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			} 
		} 
		addModule(services);
	}
	
	public void addModule(String module, Class<?>... clazz){
		Object[] services = new Object[clazz.length];
		for(int i=0;i<clazz.length;i++){
			Class<?> c = clazz[i];
			try {
				services[i] = c.newInstance();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			} 
		} 
		addModule(module, services);
	} 
	
	public void removeModule(Object... services){
		for(Object obj : services){
			for(Class<?> intf : getAllInterfaces(obj.getClass())){
				removeModule(intf.getSimpleName(), obj);
				removeModule(intf.getCanonicalName(), obj);
			} 
			removeModule(obj.getClass().getSimpleName(), obj);
			removeModule(obj.getClass().getName(), obj);
		}
	}
	
	public void removeModule(String module, Object... services){
		for(Object service: services){
			this.removeCommandTable(module, service);
		}
	}
	
	private static List<Class<?>> getAllInterfaces(Class<?> clazz){
		List<Class<?>> res = new ArrayList<Class<?>>();
		while(clazz != null){ 
			res.addAll(Arrays.asList(clazz.getInterfaces()));
			clazz = clazz.getSuperclass();
		}
		return res;
	}  
	
	private void addModuleInfo(String module, Object service){
		List<RpcMethod> rpcMethods = null;
		String serviceKey = service.getClass().getCanonicalName();
		if(object2Methods.containsKey(serviceKey)){
			rpcMethods = object2Methods.get(serviceKey); 
		} else {
			rpcMethods = new ArrayList<RpcMethod>();
			object2Methods.put(serviceKey,rpcMethods);
		}
		  
		
		Method [] methods = service.getClass().getMethods();  
		for (Method m : methods) { 
			if(m.getDeclaringClass() == Object.class) continue;
			String method = m.getName();
			Remote cmd = m.getAnnotation(Remote.class);
			if(cmd != null){ 
				method = cmd.id();
				if(cmd.exclude()) continue;
				if("".equals(method)){
					method = m.getName();
				}  
			}
			RpcMethod rpcm = null;
			for(RpcMethod rm : rpcMethods) {
				if(rm.getName().equals(method)) {
					rpcm = rm;
					break;
				}
			}
			if(rpcm != null) {
				if(!rpcm.modules.contains(module)) {
					rpcm.modules.add(module);
				}
			} else {
				List<String> modules = new ArrayList<String>(); 
				modules.add(module);
				rpcm = new RpcMethod();
				rpcm.setModules(modules);
				rpcm.setName(method);
				rpcm.setReturnType(m.getReturnType().getCanonicalName());
				List<String> paramTypes = new ArrayList<String>();
				for(Class<?> t : m.getParameterTypes()){
					paramTypes.add(t.getCanonicalName());
				}
				rpcm.setParamTypes(paramTypes);
				rpcMethods.add(rpcm);
			} 
		} 
	}
	
	private void removeModuleInfo(Object service){
		String serviceKey = service.getClass().getName();
		object2Methods.remove(serviceKey);
	} 
	
	private void initCommandTable(String module, Object service){
		addModuleInfo(module, service);
		
		try {  
			Method [] methods = service.getClass().getMethods(); 
			for (Method m : methods) { 
				if(m.getDeclaringClass() == Object.class) continue;   
				
				String method = m.getName();
				Remote cmd = m.getAnnotation(Remote.class);
				if(cmd != null){ 
					method = cmd.id();
					if(cmd.exclude()) continue;
					if("".equals(method)){
						method = m.getName();
					}  
				} 
				
				m.setAccessible(true);
				MethodInstance mi = new MethodInstance(m, service);
				
				String[] keys = paramSignature(module, m);
				for(String key : keys){
					if(this.methods.containsKey(key)){
						log.debug(key + " overrided"); 
					}  
					this.methods.put(key, mi); 
				} 
			}  
		} catch (SecurityException e) {
			log.error(e.getMessage(), e);
		}   
	}
	
	private void removeCommandTable(String module, Object service){
		removeModuleInfo(service);
		
		try {  
			Method [] methods = service.getClass().getMethods(); 
			for (Method m : methods) { 
				String method = m.getName();
				Remote cmd = m.getAnnotation(Remote.class);
				if(cmd != null){ 
					method = cmd.id();
					if(cmd.exclude()) continue;
					if("".equals(method)){
						method = m.getName();
					}  
				} 
				String[] keys = paramSignature(module, m);
				for(String key : keys){ 
					this.methods.remove(key);
				}  
			}  
		} catch (SecurityException e) {
			log.error(e.getMessage(), e);
		}   
	}  
	
	
	static class MethodMatchResult{
		MethodInstance method;
		boolean fullMatched;
	}
	
	private MethodMatchResult matchMethod(Request req){ 
		StringBuilder sb = new StringBuilder();
		if(req.getParamTypes() != null){
			for(String type : req.getParamTypes()){
				sb.append(type+",");
			}
		}
		String module = req.getModule(); 
		String method = req.getMethod();
		String key = module+":"+method+":"+sb.toString();  
		String key2 = module+":"+method;
		
		MethodMatchResult result = new MethodMatchResult();
		if(this.methods.containsKey(key)){
			result.method = this.methods.get(key); 
			result.fullMatched = true;
			return result;
		} else { 
			if(this.methods.containsKey(key2)){
				result.method = this.methods.get(key2); 
				result.fullMatched = false; 
				return result;
			}
			String errorMsg = String.format("%s:%s Not Found", module, method);
			throw new IllegalArgumentException(errorMsg); 
		}
	}
	
	private String[] paramSignature(String module, Method m){
		Class<?>[] paramTypes = m.getParameterTypes();
		StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		for(int i=0;i<paramTypes.length;i++){ 
			sb.append(paramTypes[i].getSimpleName()+",");
			sb2.append(paramTypes[i].getName()+",");
		} 
		
		String key = module + ":" + m.getName()+":"+sb.toString(); 
		String key2 = module + ":" + m.getName()+":"+sb2.toString(); 
		String key3 = module + ":" + m.getName();
		if(key.equals(key2)){
			return new String[]{ key, key3};
		}
		return new String[]{ key, key2, key3};
	}
	
	
	private void checkParamTypes(MethodInstance target, Request req){
		Class<?>[] targetParamTypes = target.method.getParameterTypes();
		int requiredLength = 0;
		for(Class<?> clazz : targetParamTypes){
			if(Message.class.isAssignableFrom(clazz)) continue; //ignore Message parameter
			requiredLength++;
		}
		if(requiredLength !=  req.getParams().length){
			String requiredParamTypeString = "";
			for(int i=0;i<targetParamTypes.length;i++){
				Class<?> paramType = targetParamTypes[i]; 
				requiredParamTypeString += paramType.getName();
				if(i<targetParamTypes.length-1){
					requiredParamTypeString += ", ";
				}
			}
			Object[] params = req.getParams();
			String gotParamsString = "";
			for(int i=0;i<params.length;i++){ 
				gotParamsString += params[i];
				if(i<params.length-1){
					gotParamsString += ", ";
				}
			}
			String errorMsg = String.format("Method:%s(%s), called with %s(%s)", 
					target.method.getName(), requiredParamTypeString, target.method.getName(), gotParamsString);
			throw new IllegalArgumentException(errorMsg);
		}
	} 
	
	public class DocRender {
		public Message index(Message request) throws IOException { 
			Message result = new Message(); 
			Map<String, Object> model = new HashMap<String, Object>();
			 
			if(!enableMethodPage){
				result.setBody("<h1>Method page disabled</h1>");
				return result;
			}
			
			String doc = "<div>";
			int rowIdx = 0;
			for(List<RpcMethod> objectMethods : object2Methods.values()) {
				for(RpcMethod m : objectMethods) {
					doc += rowDoc(m, rowIdx++);
				}
			}
			doc += "</div>";
			model.put("content", doc); 
			
			String body = FileKit.loadFile("rpc.htm", model);
			result.setBody(body);
			return result;
		}
		
		private String rowDoc(RpcMethod m, int idx) { 
			String color = "altColor";
			if(idx%2 != 0) {
				color = "rowColor";
			}
			String fmt = 
					"<tr class=\"%s\">" +  
					"<td class=\"returnType\">%s</td>" +  
					"<td class=\"methodParams\"><code><strong><a href=\"%s\">%s</a></strong>(%s)</code>" + 
					"	<ul class=\"params\"> %s </ul>" + 
					"	<div class=\"methodDesc\">%s</div>" + 
					"</td>" +
					
					"<td class=\"modules\">" + 
					"	<ul> %s </ul>" + 
					"</td></tr>";
			String methodLink = docUrlContext + m.modules.get(0) + "/" + m.name;
			String method = m.name;
			String paramList = "";
			for(String type : m.paramTypes) {
				paramList += type + ", ";
			}
			if(paramList.length() > 0) {
				paramList = paramList.substring(0, paramList.length()-2);
			}
			String paramDesc = "";
			String methodDesc = "";
			String modules = "";
			for(String module : m.modules) {
				modules += "<li>"+module+"</li>";
			}
			return String.format(fmt, color, m.returnType, methodLink, method,
					paramList, paramDesc, methodDesc, modules);
		} 
	}
	 
	public Message process(Message msg){   
		String encoding = msg.getEncoding();
		Object result = null;
		int status = RpcCodec.STATUS_OK; //assumed to be successful
		try { 
			Request req = codec.decodeRequest(msg);  
			if(req == null){
				req = new Request();
				req.setMethod("index");
				req.setModule("index");
			} 
			if(StrKit.isEmpty(req.getModule())) {
				req.setModule("index");
			}
			if(StrKit.isEmpty(req.getMethod())) {
				req.setMethod("index");
			}
			if(req.getParams() == null){
				req.setParams(new Object[0]);
			}

		
			MethodMatchResult matchResult = matchMethod(req);
			MethodInstance target = matchResult.method;
			if(matchResult.fullMatched){
				checkParamTypes(target, req);
			}
			
			Class<?>[] targetParamTypes = target.method.getParameterTypes();
			Object[] invokeParams = new Object[targetParamTypes.length];  
			Object[] reqParams = req.getParams(); 
			int j = 0;
			for(int i=0; i<targetParamTypes.length; i++){  
				if(Message.class.isAssignableFrom(targetParamTypes[i])){
					invokeParams[i] = msg;
				} else {
					if(targetParamTypes.length == 1 
					  && targetParamTypes[0] == String.class
					  && reqParams.length > 1){ //special case: url length not matched with target
						boolean hasTopic = msg.getHeader("topic") != null;
						invokeParams[i] = HttpKit.rpcUrl(msg.getUrl(), hasTopic); 
					} else {
						if(j >= reqParams.length){
							throw new IllegalArgumentException("Argument count not matched");
						}
						invokeParams[i] = codec.convert(reqParams[j], targetParamTypes[i]);
						j++;
					}
				}
			} 
			result = target.method.invoke(target.instance, invokeParams);
			if(result instanceof Message){ //special case for Message returned type
				return (Message)result;   
			} 
		} catch (InvocationTargetException e) { 
			status = RpcCodec.STATUS_APP_ERROR;
			result = e.getTargetException(); 
		} catch (Throwable e) { 
			status = RpcCodec.STATUS_APP_ERROR; 
			if(enableStackTrace){
				result = new RpcException(e.getMessage()); //Support JDK6
			} else {
				result = new RpcException(e.getMessage(), e.getCause(), false, enableStackTrace); //Require JDK7+
			}
		} 
		try {
			Message res = codec.encodeResponse(result, encoding); 
			res.setStatus(status);  
			return res;
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		} 
		return null; //should not here
	}
	
	public RpcCodec getCodec() {
		return codec;
	}

	public void setCodec(RpcCodec codec) {
		this.codec = codec;
	}  
	
	public String getDocUrlContext() {
		return docUrlContext;
	}

	public void setDocUrlContext(String docUrlContext) {
		this.docUrlContext = docUrlContext;
	}  

	public boolean isEnableStackTrace() {
		return enableStackTrace;
	}

	public void setEnableStackTrace(boolean enableStackTrace) {
		this.enableStackTrace = enableStackTrace;
	} 

	public boolean isEnableMethodPage() {
		return enableMethodPage;
	}

	public void setEnableMethodPage(boolean enableMethodPage) {
		this.enableMethodPage = enableMethodPage;
	}


	private static class MethodInstance{
		public Method method;
		public Object instance;
		
		public MethodInstance(Method method, Object instance){
			this.method = method;
			this.instance = instance;
		} 
	}
	
	public static class RpcMethod {
		private List<String> modules = new ArrayList<String>();
		private String name;
		private List<String> paramTypes = new ArrayList<String>();
		private String returnType;
		
		public List<String> getModules() {
			return modules;
		}
		public void setModules(List<String> modules) {
			this.modules = modules;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public List<String> getParamTypes() {
			return paramTypes;
		}
		public void setParamTypes(List<String> paramTypes) {
			this.paramTypes = paramTypes;
		}
		public String getReturnType() {
			return returnType;
		}
		public void setReturnType(String returnType) {
			this.returnType = returnType;
		} 
	} 
}
