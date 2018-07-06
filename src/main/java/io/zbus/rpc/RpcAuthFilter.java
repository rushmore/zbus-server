package io.zbus.rpc;

import io.zbus.auth.ApiKeyProvider;
import io.zbus.auth.AuthResult;
import io.zbus.auth.DefaultAuth;
import io.zbus.auth.RequestAuth;
import io.zbus.transport.Message;

public class RpcAuthFilter implements RpcFilter {
	private RequestAuth auth;
	
	public RpcAuthFilter(ApiKeyProvider apiKeyProvider) {
		this.auth = new DefaultAuth(apiKeyProvider);
	} 
	
	public RpcAuthFilter(RequestAuth auth) {
		this.auth = auth;
	}  
	
	@Override
	public boolean doFilter(Message request, Message response) { 
		AuthResult res = auth.auth(request);
		if(res.success) return true;
		
		response.setStatus(403); 
		response.setBody(res.message);
		
		return false;
	} 
}
