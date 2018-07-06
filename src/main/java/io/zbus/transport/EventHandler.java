package io.zbus.transport;

/**
 * Event handler type, such as Connected/Open, Disconnected/Close event
 * 
 * @author leiming.hong Jun 27, 2018
 *
 */
public interface EventHandler { 
	/**
	 * Handler entry
	 * 
	 * @throws Exception if any exception unhandled
	 */
	void handle() throws Exception;   
}