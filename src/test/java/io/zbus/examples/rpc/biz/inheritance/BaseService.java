package io.zbus.examples.rpc.biz.inheritance;

public interface BaseService<T> {
	boolean save(T t);
}
