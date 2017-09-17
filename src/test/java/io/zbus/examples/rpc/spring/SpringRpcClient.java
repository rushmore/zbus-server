package io.zbus.examples.rpc.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import io.zbus.examples.rpc.TestCases;
import io.zbus.examples.rpc.biz.InterfaceExample;
import io.zbus.rpc.bootstrap.SpringClientBootstrap;

public class SpringRpcClient {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		ApplicationContext context = new ClassPathXmlApplicationContext("SpringRpcClient.xml"); 
		InterfaceExample example = context.getBean(InterfaceExample.class);
		
		TestCases.testDynamicProxy(example);
		
		//clean it
		SpringClientBootstrap bootstrap = context.getBean(SpringClientBootstrap.class);
		bootstrap.close();
	}
}
