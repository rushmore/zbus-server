package io.zbus.examples.rpc.http;

import io.zbus.examples.rpc.biz.InterfaceExample;
import io.zbus.rpc.Request;
import io.zbus.rpc.RpcCallback;
import io.zbus.rpc.RpcInvoker;
import io.zbus.rpc.bootstrap.http.ClientBootstrap;

public class RpcClient {

	public static void main(String[] args) throws Exception {   
		ClientBootstrap b = new ClientBootstrap(); 
		b.serviceAddress("localhost:15555") 
		 .serviceToken("myrpc_service"); 
		
		RpcInvoker rpc = b.invoker(); 
		rpc.setModule("InterfaceExample");
		
		//Way 1) Raw request
		Request req = new Request(); 
		req.setMethod("plus");
		req.setParams(new Object[]{1,2});
		
		Object res = rpc.invokeSync(req);
		System.out.println("raw: " + res);
		
		//asynchronous call
		rpc.invokeAsync(Integer.class, req, new RpcCallback<Integer>() { 
			@Override
			public void onSuccess(Integer result) {  
				System.out.println("async raw: " + result);
			}
			
			@Override
			public void onError(Exception error) {
				System.err.println(error);
			}
		});
		
		
		//Way 2) More abbreviated
		int result = rpc.invokeSync(Integer.class, "plus", 1, 2);
		System.out.println("typed: " + result); 
		
		
		
		//Way 3) Dynamic proxy class, the client side only need Interface
		InterfaceExample api = rpc.createProxy(InterfaceExample.class);
		result = api.plus(1, 2); 
		System.out.println("proxy class: " + result);
		
		
		b.close(); 
	}

}
