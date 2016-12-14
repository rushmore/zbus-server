package io.zbus.net.tcp;
 
import java.io.IOException;

import io.zbus.net.Future;
import io.zbus.net.Identifiable;
import io.zbus.net.Invoker;
import io.zbus.net.IoDriver;
import io.zbus.net.Session;


public class TcpInvoker<REQ extends Identifiable, RES extends Identifiable> extends TcpClient<REQ, RES> implements Invoker<REQ, RES> {
	
	public TcpInvoker(String address, IoDriver driver){  
		 super(address, driver);
	} 
	 
	@Override
	public void sessionData(Object msg, Session sess) throws IOException { 
		
		
		super.sessionData(msg, sess);
	}
	
	@Override
	public Future<RES> invoke(REQ req) {  
		Future<RES> future = new DefaultFuture<RES>(eventGroup.next());
		
		send(req); 
		
		return future;
	}  
}
