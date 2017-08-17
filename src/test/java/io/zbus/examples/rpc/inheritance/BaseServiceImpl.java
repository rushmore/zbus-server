package io.zbus.examples.rpc.inheritance;

public class BaseServiceImpl<T> implements BaseService<T> {
	@Override
	public boolean save(T t) { 
		System.out.println(t);
		return false;
	} 
}
