package io.zbus.net.tcp;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.netty.channel.ChannelHandlerContext;
import io.zbus.net.Session;

public class TcpSession implements Session {
	private ChannelHandlerContext ctx;
	private final String id;
	private ConcurrentMap<String, Object> attributes = null;
	
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
	
	public void write(Object msg){
		ctx.writeAndFlush(msg);
	}
	
	public void writeAndFlush(Object msg){
		ctx.writeAndFlush(msg);
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
	 
	@SuppressWarnings("unchecked")
	public <V> V attr(String key) {
		if (this.attributes == null) {
			return null;
		}

		return (V) this.attributes.get(key);
	}

	public <V> void attr(String key, V value) {
		if(value == null){
			if(this.attributes != null){
				this.attributes.remove(key);
			}
			return;
		}
		if (this.attributes == null) {
			synchronized (this) {
				if (this.attributes == null) {
					this.attributes = new ConcurrentHashMap<String, Object>();
				}
			}
		}
		this.attributes.put(key, value);
	}
	
	@Override
	public String toString() { 
		return "Session ["
				+ "remote=" + getRemoteAddress()
				+ ", active=" + isActive()   
				+ "]"; 
	}
}
