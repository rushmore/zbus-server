package io.zbus.mq.server.auth;

import java.util.concurrent.ConcurrentHashMap;

public class TokenTable extends ConcurrentHashMap<String, Token> { 
	private static final long serialVersionUID = 7167983253412165928L;
	
	private boolean enabled = false;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	} 
}
