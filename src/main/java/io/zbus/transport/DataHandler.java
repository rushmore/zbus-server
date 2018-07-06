package io.zbus.transport;

/**
 * Message data callback in networking 
 * 
 * @author leiming.hong Jun 27, 2018
 *
 * @param <T> Data type
 */
public interface DataHandler<T> { 
	/**
	 * Handler entry 
	 * @param data callback message data
	 * @throws Exception unhandled exception
	 */
	void handle(T data) throws Exception;   
}