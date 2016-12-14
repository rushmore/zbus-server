package io.zbus.net.tcp;
 
import java.io.IOException;

import io.netty.util.concurrent.DefaultPromise;
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
		DefaultPromise<RES> f = new DefaultPromise<RES>(eventGroup.next()); 
		
		send(req);
		
		return null;
	}  
}
