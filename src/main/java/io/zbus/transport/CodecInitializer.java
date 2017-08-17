package io.zbus.transport;

import java.util.List;

import io.netty.channel.ChannelHandler;

public interface CodecInitializer {
	void initPipeline(List<ChannelHandler> p);
}
