package io.zbus.net.tcp;

import java.io.IOException;
import java.util.UUID;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.zbus.net.Session;

public class TcpSession extends AttributeMap implements Session {
	private ChannelHandlerContext ctx;
	private final String id; 
	
	public TcpSession(ChannelHandlerContext ctx) {
		this.ctx = ctx;
		this.id = UUID.randomUUID().toString();
	}
	
	@Override
	public String id() {
		return id;
	}
	
	public String getRemoteAddress() { 
		ctx.isRemoved();
		return ctx.channel().remoteAddress().toString();
	}
	
	public String getLocalAddress() { 
		return ctx.channel().localAddress().toString();
	}
	
	public ChannelFuture write(Object msg){
		return ctx.writeAndFlush(msg);
	}
	
	public ChannelFuture writeAndFlush(Object msg){
		return ctx.writeAndFlush(msg);
	}
	
	@Override
	public void flush() {
		ctx.flush();
	}
	
	@Override
	public void close() throws IOException {
		ctx.close();
	}
	
	@Override
	public boolean isActive() {
		return ctx.channel().isActive();
	} 
	
	@Override
	public String toString() { 
		return "Session ["
				+ "remote=" + getRemoteAddress()
				+ ", active=" + isActive()   
				+ "]"; 
	}
}
