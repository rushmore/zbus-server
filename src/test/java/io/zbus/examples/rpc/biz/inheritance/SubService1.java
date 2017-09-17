package io.zbus.examples.rpc.biz.inheritance;

import io.zbus.rpc.Remote;

@Remote
public class SubService1 extends BaseServiceImpl<Integer> implements SubServiceInterface1{

}
