package io.zbus.auth;

import io.zbus.transport.Message;

public class DefaultAuth implements RequestAuth {
	private ApiKeyProvider apiKeyProvider;
	private RequestSign requestSign = new DefaultSign();

	public DefaultAuth(ApiKeyProvider apiKeyProvider) {
		this.apiKeyProvider = apiKeyProvider;
	}
	
	public void setRequestSign(RequestSign requestSign) {
		this.requestSign = requestSign;
	}
	
	@Override
	public AuthResult auth(Message request) { 
		String apiKey = (String)request.getHeader(APIKEY);
		if(apiKey == null) return new AuthResult(false, "missing apiKey in request");
		String sign = (String)request.getHeader(SIGNATURE);
		if(sign == null) return new AuthResult(false, "missing signature in request");
		
		if(!apiKeyProvider.apiKeyExists(apiKey)) return new AuthResult(false, "apiKey not exists");
		String secretKey = apiKeyProvider.secretKey(apiKey);
		if(secretKey == null) return new AuthResult(false, "secretKey not exists");
		
		Message copy = new Message(request);
		copy.removeHeader(SIGNATURE);
		
		String sign2 = requestSign.calcSignature(copy, apiKey, secretKey);
		if(sign.equals(sign2)) {
			return new AuthResult(true);
		} else {
			return new AuthResult(false, "signature mismatched");
		}
	} 
}
