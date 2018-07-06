package io.zbus.rpc;

import io.zbus.rpc.biz.InterfaceExample;
import io.zbus.transport.Message;

public class TestCases { 
	
	public static void doTest(RpcClient rpc, String urlPrefix) throws Exception {     
		//1) 原始的方法调用中的数据格式 
		Message req = new Message();
		req.setUrl(urlPrefix+"/plus");
		req.setBody(new Object[] {1,2}); //body as parameter array
		
		Message res = rpc.invoke(req); //同步调用
		System.out.println(res);
		
		//2)纯异步API
		rpc.invoke(req, resp -> { //异步调用
			System.out.println(resp); 
		}); 
		
		//3) 动态代理
		InterfaceExample example = rpc.createProxy(urlPrefix, InterfaceExample.class);
		int c = example.plus(1, 2);
		System.out.println(c);
		
		
		//4) 参数同时基于URL的调用格式 
		req = new Message();
		req.setUrl(urlPrefix + "/plus/1/2");
		res = rpc.invoke(req); 
		System.out.println("urlbased: " + res);
	}
}
