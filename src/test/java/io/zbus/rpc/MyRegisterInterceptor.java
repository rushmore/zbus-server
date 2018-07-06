package io.zbus.rpc;

import java.util.Arrays;
import java.util.Map;

public class MyRegisterInterceptor implements RpcStartInterceptor {

	@Override
	public void onStart(RpcProcessor processor) { 
		//注册方法
		GenericService service = new GenericService();
		
		//抽象的服务调用，增加一个具体的方法
		RpcMethod spec = new RpcMethod(); 
		spec.method = "func1";
		spec.paramTypes = Arrays.asList(String.class.getName(), Integer.class.getName());
		spec.paramNames = Arrays.asList("name", "age");
		spec.returnType = Map.class.getName();
		
		processor.mount(spec, service);
	} 
}