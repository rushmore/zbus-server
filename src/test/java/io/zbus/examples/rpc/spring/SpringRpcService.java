package io.zbus.examples.rpc.spring;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringRpcService {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		new ClassPathXmlApplicationContext("SpringRpcService.xml"); 
	}
}
