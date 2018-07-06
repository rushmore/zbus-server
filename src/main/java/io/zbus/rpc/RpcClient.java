package io.zbus.rpc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import io.zbus.kit.HttpKit;
import io.zbus.kit.JsonKit;
import io.zbus.transport.Client;
import io.zbus.transport.IoAdaptor;
import io.zbus.transport.Message; 

public class RpcClient extends Client {  
	private String urlPrefix = "";
	
	public RpcClient(String address) {
		this(address, null);
	}
	public RpcClient(String address, String urlPrefix) {  
		super(address);
		this.urlPrefix = urlPrefix; 
	}   
	
	public RpcClient(IoAdaptor ioAdaptor) {
		super(ioAdaptor);
	} 
	
	public void setUrlPrefix(String urlPrefix) {
		this.urlPrefix = urlPrefix;
	}
	
	private static <T> T parseResult(Message resp, Class<T> clazz) { 
		Object data = resp.getBody();
		Integer status = resp.getStatus();
		if(status != null && status != 200){
			if(data instanceof RuntimeException){
				throw (RuntimeException)data;
			} else {
				throw new RpcException(data.toString());
			}
		} 
		try { 
			return (T) JsonKit.convert(data, clazz); 
		} catch (Exception e) { 
			throw new RpcException(e.getMessage(), e.getCause());
		}
	}  
	 
	@SuppressWarnings("unchecked")
	public <T> T createProxy(String module, Class<T> clazz){   
		String urlPrefix = HttpKit.joinPath(this.urlPrefix, module);
		Constructor<RpcInvocationHandler> rpcInvokerCtor;
		try {
			rpcInvokerCtor = RpcInvocationHandler.class.getConstructor(new Class[] {RpcClient.class, String.class }); 
			RpcInvocationHandler rpcInvokerHandler = rpcInvokerCtor.newInstance(this, urlPrefix); 
			Class<T>[] interfaces = new Class[] { clazz }; 
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			return (T) Proxy.newProxyInstance(classLoader, interfaces, rpcInvokerHandler);
		} catch (Exception e) { 
			throw new RpcException(e);
		}   
	}  
	
	
	public static class RpcInvocationHandler implements InvocationHandler {  
		private RpcClient rpc; 
		private String urlPrefix;
		private static final Object REMOTE_METHOD_CALL = new Object();

		public RpcInvocationHandler(RpcClient rpc, String urlPrefix) {
			this.rpc = rpc;
			this.urlPrefix = urlPrefix;
		}
		
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if(args == null){
				args = new Object[0];
			}
			Object value = handleLocalMethod(proxy, method, args);
			if (value != REMOTE_METHOD_CALL) return value; 
			 
			 
			String urlPath = HttpKit.joinPath(urlPrefix, method.getName());
			Message request = new Message();
			request.setUrl(urlPath);
			request.setBody(args); //use body
			
			Message resp = rpc.invoke(request);
			return parseResult(resp, method.getReturnType());
		}

		protected Object handleLocalMethod(Object proxy, Method method,
				Object[] args) throws Throwable {
			String methodName = method.getName();
			Class<?>[] params = method.getParameterTypes();

			if (methodName.equals("equals") && params.length == 1
					&& params[0].equals(Object.class)) {
				Object value0 = args[0];
				if (value0 == null || !Proxy.isProxyClass(value0.getClass()))
					return new Boolean(false);
				RpcInvocationHandler handler = (RpcInvocationHandler) Proxy.getInvocationHandler(value0);
				return new Boolean(this.rpc.equals(handler.rpc));
			} else if (methodName.equals("hashCode") && params.length == 0) {
				return new Integer(this.rpc.hashCode());
			} else if (methodName.equals("toString") && params.length == 0) {
				return "RpcInvocationHandler[" + this.rpc + "]";
			}
			return REMOTE_METHOD_CALL;
		} 
	} 
}
