package io.zbus.rpc;

import java.util.Map;

/**
 * 
 * Generic method invocation bridge
 * 
 * @author leiming.hong
 *
 */
public interface MethodInvoker {
	public Object invoke(String funcName, Map<String, Object> params); 
}
	