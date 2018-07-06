package io.zbus.rpc;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.kit.JsonKit;
import io.zbus.mq.plugin.UrlEntry;
import io.zbus.rpc.annotation.Auth;
import io.zbus.rpc.annotation.Param;
import io.zbus.rpc.annotation.RequestMapping;
import io.zbus.rpc.doc.DocRender;
import io.zbus.transport.Message;
import io.zbus.transport.http.Http;
import io.zbus.transport.http.Http.FileForm;

public class RpcProcessor {
	private static final Logger logger = LoggerFactory.getLogger(RpcProcessor.class);   
	private Map<String, MethodInstance> urlPath2MethodTable = new HashMap<>();   //path => MethodInstance  
	
	private String urlPrefix="";  //Global URL prefix
	
	private boolean docEnabled = true; 
	private String docUrlPrefix = "doc"; //Under global
	
	 
	private boolean stackTraceEnabled = true;   
	
	private RpcFilter beforeFilter;
	private RpcFilter afterFilter;
	private RpcFilter authFilter;   
	
	public RpcProcessor mount(String urlPrefix, Object service) { 
		return mount(urlPrefix, service, true, true, true);
	}
	
	public RpcProcessor mount(String urlPrefix, Object service, boolean defaultAuth) {
		return mount(urlPrefix, service, defaultAuth, true, true);
	} 
	
	@SuppressWarnings("unchecked")
	public RpcProcessor mount(String urlPrefix, Object service, boolean defaultAuth, boolean enableDoc, boolean overrideMethod) {  
		if(service instanceof List) {
			List<Object> svcList = (List<Object>)service;
			for(Object svc : svcList) {
				mount(urlPrefix, svc, defaultAuth, enableDoc, overrideMethod);
			}
			return this;
		} 
		try {
			if(service instanceof Class<?>) {
				service = ((Class<?>)service).newInstance();
			} 
			
			Method[] methods = service.getClass().getMethods();
			boolean classAuthEnabled = defaultAuth;
			Auth classAuth = service.getClass().getAnnotation(Auth.class);
			if(classAuth != null) {
				classAuthEnabled = !classAuth.exclude();
			}
			
			for (Method m : methods) {
				if (m.getDeclaringClass() == Object.class) continue;  
				
				if(Modifier.isStatic(m.getModifiers())) {
					continue;
				}
				
				RpcMethod info = new RpcMethod();
				String methodName =  m.getName();
				//default path
				String urlPath = HttpKit.joinPath(urlPrefix, methodName);
				
				info.urlPath = urlPath;
				info.method = methodName; 
				info.docEnabled = enableDoc;
				info.returnType = m.getReturnType().getCanonicalName();
				
				RequestMapping p = m.getAnnotation(RequestMapping.class);
				if (p != null) { 
					if (p.exclude()) continue; 
					info.docEnabled = enableDoc && p.docEnabled();
					info.urlAnnotation = p;
					urlPath = annoPath(p);    
					if(urlPath != null) {
						info.urlPath = HttpKit.joinPath(urlPrefix, urlPath);
					}
				} 
				
				Auth auth = m.getAnnotation(Auth.class);
				boolean authRequired = classAuthEnabled;
				if(auth != null) {
					authRequired = !auth.exclude();
				}
				info.authRequired = authRequired;

				m.setAccessible(true);  
				
				List<String> paramTypes = new ArrayList<String>();
				for (Class<?> t : m.getParameterTypes()) {
					paramTypes.add(t.getCanonicalName());
				}
				info.paramTypes = paramTypes;  
				Annotation[][] paramAnnos = m.getParameterAnnotations(); 
				int size = paramTypes.size(); 
				for(int i=0; i<size; i++) {
					Annotation[] annos = paramAnnos[i];
					for(Annotation annotation : annos) {
						if(Param.class.isAssignableFrom(annotation.getClass())) {
							Param param = (Param)annotation;  
							info.paramNames.add(param.value()); 
							break;
						}
					} 
				}  
				
				//register in tables
				mount(new MethodInstance(info, m, service), overrideMethod);  
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return this;
	} 
	
	public RpcProcessor mount(RpcMethod spec, MethodInvoker service) {
		return mount(spec, service, true);
	}
	
	public RpcProcessor mount(RpcMethod spec, MethodInvoker service, boolean overrideMethod) {
		MethodInstance mi = new MethodInstance(spec, service);  
		return mount(mi, overrideMethod);
	}
	
	public RpcProcessor mount(MethodInstance mi) {
		return mount(mi, true);
	}
	
	public RpcProcessor mount(MethodInstance mi, boolean overrideMethod) { 
		RpcMethod spec = mi.info;
		String urlPath = spec.getUrlPath();
		if(urlPath == null) {
			throw new IllegalArgumentException("urlPath can not be null");
		}   
		
		boolean exists = this.urlPath2MethodTable.containsKey(urlPath);
		if (exists) {
			if(overrideMethod) {
				logger.warn(urlPath + " overridden"); 
				this.urlPath2MethodTable.put(urlPath, mi); 
			} else {
				logger.warn(urlPath + " exists, new ignored"); 
			}
		} else {
			this.urlPath2MethodTable.put(urlPath, mi); 
		} 
		return this;
	} 
	
	public RpcProcessor unmount(String module, Object service) {
		try {
			Method[] methods = service.getClass().getMethods();
			for (Method m : methods) {
				String path = HttpKit.joinPath(module, m.getName());
				RequestMapping p = m.getAnnotation(RequestMapping.class);
				if (p != null) {
					if (p.exclude()) continue; 
					path = annoPath(p); 
				} 
				this.unmount(path); 
				this.unmount(module, m.getName());
			}
		} catch (SecurityException e) {
			logger.error(e.getMessage(), e);
		}
		return this;
	}  
	
	public RpcProcessor unmount(String urlPath) { 
		this.urlPath2MethodTable.remove(urlPath);  
		return this;
	} 
	
	public RpcProcessor unmount(String module, String method) {  
		String urlPath = HttpKit.joinPath(module, method);
		unmount(urlPath);
		return this;
	}  
	
	private String annoPath(RequestMapping p) {
		if(p.path().length() == 0) return p.value();
		return p.path();
	} 
	
	public void process(Message req, Message response) {   
		try {  
			if (req == null) {
				req = new Message();  
			}   
			
			if(beforeFilter != null) {
				boolean next = beforeFilter.doFilter(req, response);
				if(!next) return;
			} 
			
			invoke(req, response);
			
			if(afterFilter != null) {
				afterFilter.doFilter(req, response);
			} 
		} catch (Throwable e) {
			logger.info(e.getMessage(), e);  
			Object errorMsg = e.getMessage();
			if(errorMsg == null) errorMsg = e.getClass().toString(); 
			response.setBody(errorMsg);
			//response.setBody(e); 
			response.setHeader(Http.CONTENT_TYPE, "text/html; charset=utf8");
			response.setStatus(500);
		} finally {
			response.setHeader(Protocol.ID, req.getHeader(Protocol.ID)); //Id Match
			if(response.getStatus() == null) {
				response.setStatus(200);
			}
		}  
	} 
	 
	private void reply(Message response, int status, String message) {
		response.setStatus(status);
		response.setHeader(Http.CONTENT_TYPE, "text/plain; charset=utf8");
		response.setBody(message);
	}  
	
	
	private boolean checkParams(Message req, Message res, Method method, Object[] params, Object[] invokeParams) {
		Class<?>[] targetParamTypes = method.getParameterTypes();
		
		boolean reqInject = false;
		int count = 0; 
		for(Class<?> paramType : targetParamTypes) {
			if(Message.class.isAssignableFrom(paramType)) {
				reqInject = true;
				continue;
			}
			count++;
		}
		
		if(!reqInject && count != params.length) { 
			String msg = String.format("Request(Url=%s, Method=%s, Params=%s) Bad Format", req.getUrl(), method.getName(), JsonKit.toJSONString(params));
			reply(res, 400, msg);
			return false;
		}
		  
		for (int i = 0; i < targetParamTypes.length; i++) { 
			Class<?> paramType = targetParamTypes[i];
			if(Message.class.isAssignableFrom(paramType)) {
				invokeParams[i] = req;
				continue;
			}
			if(i >= params.length) {
				invokeParams[i] = null;
			} else {
				invokeParams[i] = JsonKit.convert(params[i], targetParamTypes[i]);  
			}
		} 
		return true;
	}
	
	
	private class MethodTarget{
		public MethodInstance methodInstance;
		public Object[] params;
	}
	 
	private boolean httpMethodMatached(Message req, RequestMapping anno) { 
		if(anno.method().length == 0) {
			return true;
		}
		String httpMethod = req.getMethod();
		for(String m : anno.method()) {
			if(m.equalsIgnoreCase(httpMethod)) return true;
		}
		return false;
	}
	
	private MethodTarget findMethodByUrl(Message req, Message response) {  
		String url = req.getUrl();  
		int length = 0;
		Entry<String, MethodInstance> matched = null;
		for(Entry<String, MethodInstance> e : urlPath2MethodTable.entrySet()) {
			String key = e.getKey();
			if(url.startsWith(key)) {
				if(key.length() > length) {
					length = key.length();
					matched = e; 
				}
			}
		}  
		if(matched == null) {
			reply(response, 404, String.format("Url=%s Not Found", url)); 
			return null;
		}
		
		String urlPathMatched = matched.getKey();
		
		MethodTarget target = new MethodTarget(); 
		target.methodInstance = matched.getValue();
		Object[] params = null; 
		
		//TODO more support on URL parameters 
		RequestMapping anno = target.methodInstance.info.urlAnnotation;
		if(anno != null) {
			boolean httpMethodMatched = httpMethodMatached(req, anno);
			if(!httpMethodMatched) {
				reply(response, 405, String.format("Method(%s) Not Allowd", req.getMethod())); 
				return null;
			}
		}
		
		Object body = req.getBody(); //assumed to be params 
		if(body != null) {
			if(!(body instanceof FileForm)) { //may be upload files
				params = JsonKit.convert(body, Object[].class); 
			} 
		}   
		if(params == null) { 
			String subUrl = url.substring(urlPathMatched.length());
			UrlInfo info = HttpKit.parseUrl(subUrl);
			List<Object> paramList = new ArrayList<>(info.pathList); 
			if(!info.queryParamMap.isEmpty()) {
				paramList.add(info.queryParamMap);
			}
			params = paramList.toArray();
		} 
		target.params = params; 
		return target;
	}
	
	@SuppressWarnings("unchecked")
	private void invoke0(Message req, Message response) throws Exception {    
		String url = req.getUrl();   
		if(url == null) { 
			reply(response, 400, "url required");
			return;
		}  
		
		MethodTarget target = findMethodByUrl(req, response); 
		if(target == null) return;   
		
		Object[] params = target.params; 
		MethodInstance mi = target.methodInstance;
		
		//Authentication step in if required
		if(authFilter != null && mi.info.authRequired) { 
			boolean next = authFilter.doFilter(req, response);
			if(!next) return;
		}  
		
		Object data = null;
		if(mi.reflectedMethod != null) {
			Class<?>[] targetParamTypes = mi.reflectedMethod.getParameterTypes();
			Object[] invokeParams = new Object[targetParamTypes.length];  
			
			boolean ok = checkParams(req, response, mi.reflectedMethod, params, invokeParams);
			if(!ok) return;

			data = mi.reflectedMethod.invoke(mi.instance, invokeParams);
			
		} else if(mi.target != null) {
			Map<String, Object> mapParams = new HashMap<>();  
			if(params != null) {
				if(params.length == 1 && params[0] instanceof Map) {
					mapParams = (Map<String, Object>)params[0]; 
				} else {
					for(int i=0;i <params.length; i++) {
						if(mi.info.paramNames == null) break;
						if(i<mi.info.paramNames.size()) {
							mapParams.put(mi.info.paramNames.get(i), params[i]);
						}
					}
				}
			}
			data = mi.target.invoke(mi.info.method, mapParams);
		}
		
		if(data instanceof Message) {
			response.replace((Message)data);
		} else {
			response.setStatus(200); 
			response.setHeader(Http.CONTENT_TYPE, "application/json; charset=utf8"); 
			response.setBody(data); 
		} 
	}
	 
	private void invoke(Message req, Message response) {   
		try {     
			invoke0(req, response);
		} catch (Throwable e) {  
			logger.error(e.getMessage(), e);
			Throwable t = e;
			if(t instanceof InvocationTargetException) {
				t  = ((InvocationTargetException)e).getTargetException();
				if(t == null) {
					t = e;
				}
			}  
			if(!stackTraceEnabled) {
				t.setStackTrace(new StackTraceElement[0]);
			}
			Object errorMsg = t.getMessage();
			if(errorMsg == null) errorMsg = t.getClass().toString(); 
			response.setBody(errorMsg);
			
			//response.setBody(t);
			response.setHeader(Http.CONTENT_TYPE, "text/html; charset=utf8");
			response.setStatus(500); 
		}  
	}
	
	public RpcProcessor mountDoc() { 
		if(!this.docEnabled) return this;
		DocRender render = new DocRender(this); 
		mount(docUrlPrefix, render, false, false, false);
		return this;
	}   

	public RpcProcessor setBeforeFilter(RpcFilter beforeFilter) {
		this.beforeFilter = beforeFilter;
		return this;
	} 

	public RpcProcessor setAfterFilter(RpcFilter afterFilter) {
		this.afterFilter = afterFilter;
		return this;
	} 

	public RpcProcessor setAuthFilter(RpcFilter authFilter) {
		this.authFilter = authFilter;
		return this;
	} 

	public boolean isStackTraceEnabled() {
		return stackTraceEnabled;
	}

	public RpcProcessor setStackTraceEnabled(boolean stackTraceEnabled) {
		this.stackTraceEnabled = stackTraceEnabled;
		return this;
	}

	public boolean isDocEnabled() {
		return docEnabled;
	}

	public RpcProcessor setDocEnabled(boolean docEnabled) {
		this.docEnabled = docEnabled;
		return this;
	} 

	public String getDocModule() {
		return docUrlPrefix;
	}

	public RpcProcessor setDocModule(String docModule) {
		this.docUrlPrefix = docModule;
		return this;
	}  
	
	@SuppressWarnings("unchecked")
	public void setModuleTable(Map<String, Object> instances){
		if(instances == null) return;
		for(Entry<String, Object> e : instances.entrySet()){
			Object svc = e.getValue();
			if(svc instanceof List) {
				mount(e.getKey(), (List<Object>)svc); 
			} else {
				mount(e.getKey(), svc);
			}
		}
	}
	
	public RpcProcessor setUrlPrefix(String urlPrefix) {
		this.urlPrefix = urlPrefix;
		return this;
	} 
	
	public String getUrlPrefix() {
		return urlPrefix;
	} 
	
	public String getDocUrlPrefix() {
		return docUrlPrefix;
	}

	public void setDocUrlPrefix(String docUrlPrefix) {
		this.docUrlPrefix = docUrlPrefix;
	}

	public List<RpcMethod> rpcMethodList() { 
		List<RpcMethod> res = new ArrayList<>();
		TreeMap<String, MethodInstance> methods = new TreeMap<>(this.urlPath2MethodTable);
		Iterator<Entry<String, MethodInstance>> iter = methods.entrySet().iterator();
		while(iter.hasNext()) {
			MethodInstance mi = iter.next().getValue();
			res.add(mi.info); 
		} 
		return res;
	}
	
	public List<UrlEntry> urlEntryList(String mq){
		List<RpcMethod> methods = rpcMethodList();
		
		List<UrlEntry> entries = new ArrayList<>();
		for(RpcMethod method : methods) {
			UrlEntry e = new UrlEntry(); 
			e.url = HttpKit.joinPath(urlPrefix, method.urlPath);
			e.mq = mq;
			entries.add(e);
		} 
		return entries;
	}
	
	public static class MethodInstance {
		public RpcMethod info = new RpcMethod();    
		
		//Mode1 reflection method of class
		public Method reflectedMethod;
		public Object instance;    
		
		//Mode2 proxy to target
		public MethodInvoker target;      
		
		public MethodInstance(RpcMethod info, MethodInvoker target) {
			if(info.method == null) {
				throw new IllegalArgumentException("method required");
			} 
			this.info = info; 
			this.target = target;
		}
		
		public MethodInstance(RpcMethod info, Method reflectedMethod, Object instance) {
			this.reflectedMethod = reflectedMethod;
			this.instance = instance; 
			this.info = info; 
			if(info.method == null) {
				this.info.method = reflectedMethod.getName(); 
			}  
		} 
	}
}
