package io.zbus.transport.ipc;

import java.io.IOException;

import io.zbus.transport.AbstractClient;
import io.zbus.transport.Id;


public class IpcClient<REQ extends Id, RES extends Id> extends AbstractClient<REQ, RES> { 
	
	protected String serverAddress() {
		return "ipc://named_pipe|unix_sock";
	}
 
	public synchronized void connectAsync() { 
		throw new IllegalStateException("Not implemented yet");
	} 

	@Override
	public void connectSync(long timeout) throws IOException, InterruptedException {
		 
	} 
}

