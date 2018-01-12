package io.zbus.rpc.transport.mq;

import java.io.IOException;

import io.zbus.mq.Message;
import io.zbus.mq.MessageHandler;
import io.zbus.mq.MqClient;
import io.zbus.rpc.RpcProcessor;

public class RpcMessageHandler implements MessageHandler {
	
	private final RpcProcessor rpcProcessor;
	
	public RpcMessageHandler(RpcProcessor rpcProcessor) {
		this.rpcProcessor = rpcProcessor;
	}
	
	@Override
	public void handle(Message msg, MqClient client) throws IOException { 
		final String topic = msg.getTopic();
		final String msgId  = msg.getId();
		final String sender = msg.getSender();
		 
		io.zbus.transport.http.Message httpMsg = rpcProcessor.process(msg);
		Message res = new Message(httpMsg);
		if(res != null){
			res.setId(msgId);
			res.setTopic(topic);  
			res.setReceiver(sender);  
			if(res.getStatus() == null){
				res.setStatus(200); //default to 200, if not set
			}
			//route back message
			client.route(res);
		}
	} 
}
