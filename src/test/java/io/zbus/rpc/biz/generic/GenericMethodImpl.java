package io.zbus.rpc.biz.generic;

import io.zbus.rpc.annotation.RequestMapping;

@RequestMapping
public class GenericMethodImpl implements GenericMethod{

	@Override
	public <T> void test(T t) { 
		System.out.println(t);
	}

}
