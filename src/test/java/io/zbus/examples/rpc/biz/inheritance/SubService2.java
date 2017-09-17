package io.zbus.examples.rpc.biz.inheritance;

import io.zbus.rpc.Remote;

@Remote
public class SubService2 extends BaseServiceImpl<String> implements SubServiceInterface2 {
	@Override
	public boolean save(String t) {
		System.out.println("override! " + t);
		return true;
	}
}
