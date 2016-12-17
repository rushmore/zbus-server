package io.zbus.mq.server;

import io.zbus.mq.net.MessageServer;

public class MqServer extends MessageServer{  
	private MqAdaptor adaptor = new MqAdaptor();
	
	public void start(int port){
		this.start(port, adaptor);
	}
}
