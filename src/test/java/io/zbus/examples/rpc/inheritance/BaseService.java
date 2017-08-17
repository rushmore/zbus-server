package io.zbus.examples.rpc.inheritance;

public interface BaseService<T> {
	boolean save(T t);
}
