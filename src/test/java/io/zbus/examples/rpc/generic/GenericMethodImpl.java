package io.zbus.examples.rpc.generic;

public class GenericMethodImpl implements GenericMethod{

	@Override
	public <T> void test(T t) { 
		System.out.println(t);
	}

}
