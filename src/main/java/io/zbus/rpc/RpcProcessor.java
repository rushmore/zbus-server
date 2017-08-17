package io.zbus.rpc;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.zbus.kit.StrKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Message;
import io.zbus.mq.MessageHandler;
import io.zbus.mq.MqClient;


public class RpcProcessor implements MessageHandler{
	private static final Logger log = LoggerFactory.getLogger(RpcProcessor.class); 
	
	private RpcCodec codec = new JsonRpcCodec();
	private Map<String, MethodInstance> methods = new HashMap<String, MethodInstance>();
	
	private Map<String, List<RpcMethod>> object2Methods = new HashMap<String, List<RpcMethod>>();
	
	 
	public void addModule(Object... services){
		for(Object obj : services){
			for(Class<?> intf : getAllInterfaces(obj.getClass())){
				addModule(intf.getSimpleName(), obj);
				addModule(intf.getName(), obj);
			}
			addModule("", obj);
			addModule(obj.getClass().getSimpleName(), obj);
			addModule(obj.getClass().getName(), obj);
		} 
	}
	
	public void addModule(String module, Object... services){
		for(Object service: services){
			this.initCommandTable(module, service);
		}
	} 
	
	public void removeModule(Object... services){
		for(Object obj : services){
			for(Class<?> intf : getAllInterfaces(obj.getClass())){
				removeModule(intf.getSimpleName(), obj);
				removeModule(intf.getCanonicalName(), obj);
			}
			removeModule("", obj);
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
	
	private void addMoudleInfo(Object service){
		String serviceKey = service.getClass().getCanonicalName();
		if(object2Methods.containsKey(serviceKey)){
			return;
		}
		List<String> modules = new ArrayList<String>(); 
		modules.add(""); 
		for(Class<?> intf : getAllInterfaces(service.getClass())){
			modules.add(intf.getSimpleName());
			modules.add(intf.getCanonicalName()); 
		}
		modules.add(service.getClass().getSimpleName()); 
		modules.add(service.getClass().getName());  
		
		Method [] methods = service.getClass().getMethods(); 
		List<RpcMethod> rpcMethods = new ArrayList<RpcMethod>();
		object2Methods.put(serviceKey,rpcMethods);
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
			RpcMethod rpcm = new RpcMethod();
			rpcm.setModules(modules);
			rpcm.setName(method);
			rpcm.setReturnType(m.getReturnType().getName());
			List<String> paramTypes = new ArrayList<String>();
			for(Class<?> t : m.getParameterTypes()){
				paramTypes.add(t.getName());
			}
			rpcm.setParamTypes(paramTypes);
			rpcMethods.add(rpcm);
		} 
	}
	
	private void removeMoudleInfo(Object service){
		String serviceKey = service.getClass().getName();
		object2Methods.remove(serviceKey);
	} 
	
	private void initCommandTable(String module, Object service){
		addMoudleInfo(service);
		
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
		removeMoudleInfo(service);
		
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
	
	private MethodInstance matchMethod(Request req){ 
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
		if(this.methods.containsKey(key)){
			return this.methods.get(key); 
		} else { 
			if(this.methods.containsKey(key2)){
				return this.methods.get(key2);
			}
			String errorMsg = String.format("%s:%s not found, missing moudle settings?", module, method);
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
		
		if(targetParamTypes.length !=  req.getParams().length){
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
	
	@Override
	public void handle(Message msg, MqClient client) throws IOException {
		final String mq = msg.getTopic();
		final String msgId  = msg.getId();
		final String sender = msg.getSender();
		 
		Message res = process(msg);
		
		if(res != null){
			res.setId(msgId);
			res.setTopic(mq);  
			res.setReceiver(sender);  
			//route back message
			client.route(res);
		}
	}
	
	public Message process(Message msg){  
		Response resp = new Response(); 
		final String msgId = msg.getId();
		String encoding = msg.getEncoding();
		try {
			Object result = null;
			Request req = codec.decodeRequest(msg);  
			if(StrKit.isEmpty(req.getMethod())){
				result = object2Methods;
			} else {
				MethodInstance target = matchMethod(req);
				checkParamTypes(target, req);
				
				Class<?>[] targetParamTypes = target.method.getParameterTypes();
				Object[] invokeParams = new Object[targetParamTypes.length];  
				Object[] reqParams = req.getParams(); 
				for(int i=0; i<targetParamTypes.length; i++){   
					invokeParams[i] = codec.convert(reqParams[i], targetParamTypes[i]);
				} 
				result = target.method.invoke(target.instance, invokeParams);
			} 
			resp.setResult(result);  
				
		} catch (InvocationTargetException e) { 
			resp.setError(e.getTargetException()); 
		} catch (Throwable e) { 
			resp.setError(e); 
		} 
		try {
			Message res = codec.encodeResponse(resp, encoding);
			res.setId(msgId); 
			res.setStatus(200);
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
