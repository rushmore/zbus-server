package io.zbus.transport.http;

import java.util.List;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.zbus.transport.CodecInitializer;
import io.zbus.transport.EventLoop;
import io.zbus.transport.tcp.TcpServer;

public class MessageServer extends TcpServer { 
	public MessageServer() {
		this(null);
	}

	public MessageServer(final EventLoop loop) {
		super(loop);  
		codec(new CodecInitializer() {
			@Override
			public void initPipeline(List<ChannelHandler> p) {
				p.add(new HttpServerCodec());
				p.add(new HttpObjectAggregator(getEventLoop().getPackageSizeLimit()));
				p.add(new MessageCodec());
			}
		}); 
	} 
}
