package io.zbus.mq;

import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;


public class MessageCodec extends MessageToMessageCodec<Object, Message> { 
	@Override
	protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> out) throws Exception { 
		out.add(msg);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, Object obj, List<Object> out) throws Exception {
		if(obj instanceof io.zbus.transport.http.Message){
			Message msg = new Message((io.zbus.transport.http.Message)obj);
			out.add(msg);
		}
	} 
}
