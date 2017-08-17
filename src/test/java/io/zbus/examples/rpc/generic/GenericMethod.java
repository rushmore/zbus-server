package io.zbus.examples.rpc.generic;

public interface GenericMethod {
	<T> void test(T t);
}
