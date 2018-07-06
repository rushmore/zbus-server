package io.zbus.auth;

public class AuthResult {
	public boolean success;
	public String message;
	
	public AuthResult(){}
	public AuthResult(boolean success) {
		this.success = success;
	}
	
	public AuthResult(boolean success, String message) {
		this.success = success;
		this.message = message;
	}
}
