package io.zbus.rpc.server;

import java.io.IOException;

import io.zbus.mq.Protocol;
import io.zbus.rpc.RpcProcessor;
import io.zbus.transport.Message;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session;

public class HttpRpcServerAdaptor extends ServerAdaptor {
	protected RpcProcessor processor;

	public HttpRpcServerAdaptor(RpcProcessor processor) {
		this.processor = processor;
	} 
	
	public void setProcessor(RpcProcessor processor) {
		this.processor = processor;
	}

	@Override
	public void onMessage(Object msg, Session sess) throws IOException {
		Message request = null;  
		if (!(msg instanceof Message)) { 
			throw new IllegalStateException("Not support message type");
		}
		request = (Message) msg;  
		
		if(Protocol.PING.equals(request.getHeader(Protocol.CMD))) {
			return; //ignore
		}
		Message response = new Message();
		processor.process(request, response);
		if(response.getStatus() == null) {
			response.setStatus(200);
		}
		sess.write(response);
	} 
}