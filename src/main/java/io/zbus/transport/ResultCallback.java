package io.zbus.transport;

/**
 * Asynchronous message callback
 * @author rushmore (洪磊明)
 *
 * @param <T> returned message type
 */
public interface ResultCallback<T> { 
	public void onReturn(T result);  
}