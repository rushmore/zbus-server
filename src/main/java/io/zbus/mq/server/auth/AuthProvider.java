package io.zbus.mq.server.auth;

import io.zbus.mq.Message;


public interface AuthProvider {
	/**
	 * Authenticate the request message
	 * 
	 * Since zbus is stateless request-reply model, authentication occurs on every request,
	 * a good cache of authentication backend is strongly recommended.
	 * 
	 * @param message request message
	 * @return true if passed auth, false otherwise
	 */
	boolean auth(Message message); 
	
	Token getToken(String token);
	
	void addToken(Token token);
	
	void setEnabled(boolean enabled);
}
