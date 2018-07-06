package io.zbus.rpc.biz.inheritance;

import io.zbus.rpc.annotation.RequestMapping;

@RequestMapping
public class SubService1 extends BaseServiceImpl<Integer> implements SubServiceInterface1{

}
