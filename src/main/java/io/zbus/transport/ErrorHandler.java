package io.zbus.transport;

/**
 * 
 * Error callback in networking 
 * 
 * @author leiming.hong Jun 27, 2018
 *
 */
public interface ErrorHandler { 
	/**
	 * Handler entry, user should handle all exceptions
	 * 
	 * @param e error throwed
	 */
	void handle(Throwable e);   
}