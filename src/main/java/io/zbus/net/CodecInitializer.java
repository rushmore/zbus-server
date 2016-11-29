package io.zbus.net;

import java.util.List;

import io.netty.channel.ChannelHandler;

public interface CodecInitializer {
	void initPipeline(List<ChannelHandler> p);
}
