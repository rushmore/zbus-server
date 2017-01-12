package io.zbus.mq.net;

import java.util.List;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.zbus.net.CodecInitializer;
import io.zbus.net.EventDriver;
import io.zbus.net.tcp.TcpServer;

public class MessageServer extends TcpServer { 
	public MessageServer() {
		this(null);
	}

	public MessageServer(final EventDriver driver) {
		super(driver);  
		codec(new CodecInitializer() {
			@Override
			public void initPipeline(List<ChannelHandler> p) {
				p.add(new HttpServerCodec());
				p.add(new HttpObjectAggregator(getEventDriver().getPackageSizeLimit()));
				p.add(new MessageToHttpWsCodec());
			}
		}); 
	} 
}
