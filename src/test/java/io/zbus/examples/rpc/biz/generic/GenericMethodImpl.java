package io.zbus.examples.rpc.biz.generic;

import io.zbus.rpc.Remote;

@Remote
public class GenericMethodImpl implements GenericMethod{

	@Override
	public <T> void test(T t) { 
		System.out.println(t);
	}

}
