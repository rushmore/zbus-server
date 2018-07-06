package io.zbus.mq.plugin;

import java.util.List;

import io.zbus.transport.Message;

/**
 * 
 * Message URL filter
 * 
 * return response message if not null, and it will stop handle the request message further.
 * 
 * 
 * @author leiming.hong Jul 4, 2018
 *
 */
public interface UrlFilter {
	/**
	 * Filter on Message
	 * 
	 * @param req request Message 
	 * @return Message to reply if not null
	 */
	Message doFilter(Message req);
	
	/**
	 * Update entries for URL filtering
	 * 
	 * @param entries UrlEntry list
	 * @param clear clear entries for same MQ to fully update if set true, do delta updates otherwise.
	 */
	void updateUrlEntry(String mq, List<UrlEntry> entries, boolean clear);
}
