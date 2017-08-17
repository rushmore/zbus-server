package io.zbus.transport.tcp;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.zbus.transport.AttributeMap;
import io.zbus.transport.IoAdaptor;
import io.zbus.transport.Session;

@Sharable
public class NettyAdaptor extends ChannelInboundHandlerAdapter {
	protected final static AttributeKey<String> sessionKey = AttributeKey.valueOf("session");
	protected Map<String, Session> sessionMap = new ConcurrentHashMap<String, Session>();
	protected final IoAdaptor ioAdaptor; 
	
	public NettyAdaptor(IoAdaptor ioAdaptor){
		this.ioAdaptor = ioAdaptor;
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Session sess = getSession(ctx);
		ioAdaptor.onMessage(msg, sess);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		Session sess = getSession(ctx);
		ioAdaptor.onError(cause, sess);
	}
	 
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		Session sess = attachSession(ctx); 
		ioAdaptor.sessionCreated(sess);
	}  
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Session sess = getSession(ctx);
		ioAdaptor.sessionToDestroy(sess);
		sessionMap.remove(sess.id());
	} 
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			Session sess = getSession(ctx);
			ioAdaptor.onIdle(sess);
        }
	} 
	
	private Session attachSession(ChannelHandlerContext ctx){
		Session sess = new NettySession(ctx);
		Attribute<String> attr = ctx.channel().attr(sessionKey); 
		attr.set(sess.id()); 
		sessionMap.put(sess.id(), sess);
		return sess;
	}
	
	private Session getSession(ChannelHandlerContext ctx){
		Attribute<String> attr = ctx.channel().attr(sessionKey); 
		if(attr.get() == null){
			throw new IllegalThreadStateException("Missing sessionKey");
		}
		Session sess = sessionMap.get(attr.get()); 
		if(sess == null){
			throw new IllegalThreadStateException("Session and ChannelHandlerContext mapping not found");
		}
		return sess;
	}
}

class NettySession extends AttributeMap implements Session {
	private ChannelHandlerContext ctx;
	private final String id; 
	
	public NettySession(ChannelHandlerContext ctx) {
		this.ctx = ctx;
		this.id = UUID.randomUUID().toString();
	}
	
	@Override
	public String id() {
		return id;
	}
	
	public String remoteAddress() { 
		ctx.isRemoved();
		return ctx.channel().remoteAddress().toString();
	}
	
	public String localAddress() { 
		return ctx.channel().localAddress().toString();
	}
	
	public void write(Object msg){
		ctx.writeAndFlush(msg);
	} 
	 
	@Override
	public void close() throws IOException {
		ctx.close();
	}
	
	@Override
	public boolean active() {
		return ctx.channel().isActive();
	} 
	
	@Override
	public String toString() { 
		return "Session ["
				+ "remote=" + remoteAddress()
				+ ", active=" + active()   
				+ "]"; 
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NettySession other = (NettySession) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}  
}
