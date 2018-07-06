package io.zbus.rpc.biz.inheritance;

import io.zbus.rpc.annotation.RequestMapping;

@RequestMapping
public class SubService2 extends BaseServiceImpl<String> implements SubServiceInterface2 {
	@Override
	public boolean save(String t) {
		System.out.println("override! " + t);
		return true;
	}
}
