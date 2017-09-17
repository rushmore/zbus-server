package io.zbus.transport;

import java.io.Closeable;
import java.io.IOException;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;

public class EventLoop implements Closeable { 
	private EventLoopGroup bossGroup;  
	private EventLoopGroup workerGroup;  
	private final boolean ownBossGroup;
	private final boolean ownWorkerGroup; 
	
	private SslContext sslContext; 
	private int idleTimeInSeconds = 180; //180s 
	private int packageSizeLimit = 1024*1024*1024; //maximum of 1G

	public EventLoop() {
		try {
			bossGroup = workerGroup = new NioEventLoopGroup(); 
			ownBossGroup = true;
			ownWorkerGroup = false;
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	public EventLoop(EventLoopGroup group){
		this.bossGroup = group;
		this.workerGroup = group;
		this.ownBossGroup = false;
		this.ownWorkerGroup = false;
	}

	public EventLoop(EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
		this.bossGroup = bossGroup;
		this.workerGroup = workerGroup;
		this.ownBossGroup = false;
		this.ownWorkerGroup = false;
	}
	
	public EventLoop(EventLoop loop){
		this(loop.bossGroup, loop.workerGroup);
		this.idleTimeInSeconds = loop.idleTimeInSeconds;
		this.packageSizeLimit = loop.packageSizeLimit;
		this.sslContext = loop.sslContext;
	}

	public EventLoop duplicate(){ 
		return new EventLoop(this); 
	}
	
	public EventLoopGroup getBossGroup() {
		return bossGroup;
	}


	public EventLoopGroup getWorkerGroup() {
		return workerGroup;
	} 
	
	public EventLoopGroup getGroup() {
		// try bossGroup first
		if (bossGroup != null)
			return bossGroup;
		//then workerGroup
		return workerGroup;
	}

	public SslContext getSslContext() {
		return sslContext;
	}
	
	public boolean isSslEnabled() {
		return sslContext != null;
	} 

	public void setSslContext(SslContext sslContext) { 
		this.sslContext = sslContext;
	} 
	 
	@Override
	public void close() throws IOException {
		if (ownBossGroup && bossGroup != null) {
			bossGroup.shutdownGracefully(); 
			bossGroup = null;
		}
		if (ownWorkerGroup && workerGroup != null) {
			workerGroup.shutdownGracefully();
			workerGroup = null;
		}
	}

	public int getIdleTimeInSeconds() {
		return idleTimeInSeconds;
	}

	public void setIdleTimeInSeconds(int idleTimeInSeconds) {
		this.idleTimeInSeconds = idleTimeInSeconds;
	}

	public int getPackageSizeLimit() {
		return packageSizeLimit;
	}

	public void setPackageSizeLimit(int packageSizeLimit) {
		this.packageSizeLimit = packageSizeLimit;
	}   
	
}
