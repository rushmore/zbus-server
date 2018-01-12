/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.zbus.rpc;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import io.zbus.kit.JsonKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory; 
import io.zbus.transport.ResultCallback;
import io.zbus.transport.http.Message;

public class RpcInvoker {  
	private static final Logger log = LoggerFactory.getLogger(RpcInvoker.class);  
	private String module;  
	private String encoding = "UTF-8";  
	private int timeout;
	private boolean verbose;
	
	private RpcCodec codec; 
	private MessageInvoker messageInvoker;
	
	public RpcInvoker(RpcConfig config) {
		this.messageInvoker = config.getMessageInvoker();
		this.module = config.getModule(); 
		this.codec = config.getCodec();
		this.timeout = config.getTimeout();
		this.verbose = config.isVerbose();
		this.encoding = config.getEncoding();
		if(this.codec == null) {
			this.codec = new JsonRpcCodec();
		}
	}
	 
	public RpcInvoker(RpcInvoker other){ 
		this.codec = other.codec;
		this.module = other.module;
		this.encoding = other.encoding;
		this.timeout = other.timeout;
		this.verbose = other.verbose; 
		this.messageInvoker = other.messageInvoker;
	} 
	
	public RpcCodec getCodec() {
		return codec;
	}
	
	public <T> T invokeSync(Class<T> resultClass, String method, Object... args){
		Request request = new Request()
			.module(module)
			.method(method)  
			.params(args);

		return invokeSync(resultClass, request);
	}
	
	public <T> T invokeSync(Class<T> resultClass, String method, Class<?>[] paramTypes, Object... args){
		Request request = new Request()
			.module(module)
			.method(method) 
			.paramTypes(paramTypes)
			.params(args);
	
		return invokeSync(resultClass, request);
	} 
	
	public <T> T invokeSync(Class<T> resultClass, Request request){
		Message msg = invokeSync0(request); 
		return extractResult(msg, resultClass); 
	}
	
	
	public Object invokeSync(String method, Object... args) {	
		return invokeSync(method, null, args);
	}  
	
	public Object invokeSync(String method, Class<?>[] types, Object... args) {	
		Request req = new Request()
			.module(module)
			.method(method) 
			.paramTypes(types)
			.params(args); 
		Message msg = invokeSync0(req);  
		return extractResult(msg, Object.class);
	} 
	
	public Object invokeSync(Request request) {
		Message msg = invokeSync0(request);  
		return extractResult(msg, Object.class);
	}
	
	public Message invokeSync0(Request request){
		if(request.getModule() == null || "".equals(request.getModule())) {
			request.setModule(this.module);
		}
		Message msgReq = null, msgRes = null;
		try {
			long start = System.currentTimeMillis();
			msgReq = codec.encodeRequest(request, encoding); 
			if(verbose){
				log.info("[REQ]: %s", msgReq);
			} 
			
			msgRes = messageInvoker.invokeSync(msgReq, this.timeout); 
			
			if(verbose){
				long end = System.currentTimeMillis();
				log.info("[REP]: Time cost=%dms\n%s",(end-start), msgRes);
			} 
			
		} catch (IOException e) {
			throw new RpcException(e.getMessage(), e);
		} catch (InterruptedException e) {
			throw new RpcException(e.getMessage(), e);
		}
		
		if (msgRes == null) { 
			String errorMsg = String.format("module(%s)-method(%s) request timeout\n%s", 
					module, request.getMethod(), msgReq.toString());
			throw new RpcException(errorMsg);
		}
		
		return msgRes;
	}

	
	public <T> void invokeAsync(final Class<T> clazz, Request request, final RpcCallback<T> callback){
		final long start = System.currentTimeMillis();
		final Message msgReq = codec.encodeRequest(request, encoding); 
		if(verbose){
			log.info("[REQ]: %s", msgReq);
		}  
		try {
			messageInvoker.invokeAsync(msgReq, new ResultCallback<Message>() { 
				@Override
				public void onReturn(Message result) { 
					if(verbose){
						long end = System.currentTimeMillis();
						log.info("[REP]: Time cost=%dms\n%s",(end-start), result); 
					} 
					try{
						T res = extractResult(result, clazz);
						if(callback != null){
							callback.onSuccess(res);
						}
					} catch (Exception e) {
						callback.onError(e);
					}
				}
			});
			
		} catch (IOException e) {
			throw new RpcException(e.getMessage(), e);
		}  
	}
	 
	 
	@SuppressWarnings("unchecked")
	private <T> T extractResult(Message message, Class<T> clazz){
		if(Message.class.isAssignableFrom(clazz)) return (T)message; //special case
	
		Object result = codec.decodeResponse(message);  
		if(message.getStatus() != RpcCodec.STATUS_OK){
			if(result instanceof RuntimeException){
				throw (RuntimeException)result;
			} else {
				throw new RpcException(result.toString());
			}
		}
		
		try { 
			return (T) JsonKit.convert(result, clazz); 
		} catch (Exception e) { 
			throw new RpcException(e.getMessage(), e.getCause());
		}
	}  
	
	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public MessageInvoker getMessageInvoker() {
		return messageInvoker;
	}

	public void setMessageInvoker(MessageInvoker messageInvoker) {
		this.messageInvoker = messageInvoker;
	}

	public void setCodec(RpcCodec codec) {
		this.codec = codec;
	}

	@SuppressWarnings("unchecked")
	public <T> T createProxy(Class<T> clazz){  
		Constructor<RpcInvocationHandler> rpcInvokerCtor;
		try {
			rpcInvokerCtor = RpcInvocationHandler.class.getConstructor(new Class[] {RpcInvoker.class });
			RpcInvoker rpcInvoker = new RpcInvoker(this);
			rpcInvoker.module = clazz.getName();
			RpcInvocationHandler rpcInvokerHandler = rpcInvokerCtor.newInstance(rpcInvoker); 
			Class<T>[] interfaces = new Class[] { clazz }; 
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			return (T) Proxy.newProxyInstance(classLoader, interfaces, rpcInvokerHandler);
		} catch (Exception e) { 
			throw new RpcException(e);
		}   
	} 
	
	@SuppressWarnings("unchecked")
	public static <T> T createProxy(Class<T> clazz, RpcConfig config){  
		Constructor<RpcInvocationHandler> rpcInvokerCtor;
		try {
			rpcInvokerCtor = RpcInvocationHandler.class.getConstructor(new Class[] {RpcInvoker.class });
			RpcInvoker rpcInvoker = new RpcInvoker(config);
			if(rpcInvoker.module == null || "".equals(rpcInvoker.module)) {
				rpcInvoker.module = clazz.getName();
			}   
			
			RpcInvocationHandler rpcInvokerHandler = rpcInvokerCtor.newInstance(rpcInvoker); 
			Class<T>[] interfaces = new Class[] { clazz }; 
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			return (T) Proxy.newProxyInstance(classLoader, interfaces, rpcInvokerHandler);
		} catch (Exception e) { 
			throw new RpcException(e);
		}   
	} 
	
	public static class RpcInvocationHandler implements InvocationHandler {  
		private RpcInvoker rpc; 
		private static final Object REMOTE_METHOD_CALL = new Object();

		public RpcInvocationHandler(RpcInvoker rpc) {
			this.rpc = rpc;
		}
		
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if(args == null){
				args = new Object[0];
			}
			Object value = handleLocalMethod(proxy, method, args);
			if (value != REMOTE_METHOD_CALL) return value; 
			Class<?> returnType = method.getReturnType(); 
			return rpc.invokeSync(returnType, method.getName(),method.getParameterTypes(), args);
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
