package io.zbus.rpc;

import io.zbus.transport.Message;

public interface RpcFilter { 
	/**
	 * 
	 * @param request
	 * @param response
	 * @return true if continue to handle request response
	 */
	boolean doFilter(Message request, Message response);
}
