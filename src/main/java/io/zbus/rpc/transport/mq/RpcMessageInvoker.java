package io.zbus.rpc.transport.mq;

import java.io.IOException;

import io.zbus.mq.Producer;
import io.zbus.mq.ProducerConfig;
import io.zbus.rpc.MessageInvoker;
import io.zbus.transport.ResultCallback;
import io.zbus.transport.http.Message; 

public class RpcMessageInvoker implements MessageInvoker {
	private final Producer producer;
	private final String topic; 
	
	public RpcMessageInvoker(ProducerConfig config, String topic){ 
		this.topic = topic;
		if(this.topic == null){
			throw new IllegalArgumentException("Missing topic in config");
		}
		this.producer = new Producer(config); 
	}
	
	@Override
	public Message invokeSync(Message req, int timeout) throws IOException, InterruptedException { 
		io.zbus.mq.Message message = new io.zbus.mq.Message(req);
		message.setAck(false);
		message.setTopic(this.topic);
		return this.producer.publish(message, timeout); 
	}

	@Override
	public void invokeAsync(Message req, final ResultCallback<Message> callback) throws IOException {   
		io.zbus.mq.Message message = new io.zbus.mq.Message(req);
		message.setAck(false);
		message.setTopic(this.topic);
		
		this.producer.publishAsync(message, new ResultCallback<io.zbus.mq.Message>() { 
			@Override
			public void onReturn(io.zbus.mq.Message result) { 
				callback.onReturn(result);
			}
		});
	}  
}
