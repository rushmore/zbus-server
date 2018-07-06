package io.zbus.transport;

/**
 * 
 * Provides an opportunity to adding/removing info of <code>Message</code>
 * 
 * @author leiming.hong Jun 27, 2018
 *
 */
public interface MessageInterceptor {  
	void intercept(Message message);
}
