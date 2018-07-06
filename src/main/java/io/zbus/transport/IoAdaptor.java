package io.zbus.transport;

import java.io.IOException;
 
/**
 * 
 * IO event-driven extension points, handling server/client networking events 
 *  
 * @author leiming.hong Jun 27, 2018
 *
 */
public interface IoAdaptor{   
	/**
	 * Triggered when session is created
	 * 
	 * @param sess session object
	 * @throws IOException if not handled well for IO
	 */
	void sessionCreated(Session sess) throws IOException; 
	/**
	 * Triggered when session is trying to close/disconnect
	 * 
	 * @param sess session object
	 * @throws IOException if not handled well for IO
	 */
	void sessionToDestroy(Session sess) throws IOException;  
	/**
	 * Triggered when underlying session received messages.
	 * 
	 * Use <code>Session</code> to write message if write is required
	 * 
	 * @param msg message decoded from wire
	 * @param sess session object
	 * @throws IOException if not handled well for IO
	 */
	void onMessage(Object msg, Session sess) throws IOException;  
	/**
	 * Triggered when underlying session encountered an error, such as disconnection violently from remote
	 * 
	 * @param e exception throwed from underlying session
	 * @param sess session object
	 */
	void onError(Throwable e, Session sess); 
	/**
	 * Triggered when session has been idled for a configured time span.
	 * 
	 * @param sess session object
	 * @throws IOException if not handled well for IO
	 */
	void onIdle(Session sess) throws IOException;  
}
