package io.zbus.net.tcp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.zbus.net.IoAdaptor;
import io.zbus.net.Session;

@Sharable
public class NettyToIoAdaptor extends ChannelInboundHandlerAdapter {
	private final static AttributeKey<String> sessionKey = AttributeKey.valueOf("session");
	private Map<String, Session> sessionMap = new ConcurrentHashMap<String, Session>();

	private final IoAdaptor ioAdaptor; 
	public NettyToIoAdaptor(IoAdaptor ioAdaptor){
		this.ioAdaptor = ioAdaptor;
	}
	
	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception { 
		super.channelRegistered(ctx);
		Session sess = attachSession(ctx); 
		ioAdaptor.sessionRegistered(sess);
	}
	
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		super.channelUnregistered(ctx);
		Session sess = getSession(ctx);
		ioAdaptor.sessionUnregistered(sess);
		sessionMap.remove(sess.id());
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Session sess = getSession(ctx);
		ioAdaptor.sessionData(msg, sess);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		Session sess = getSession(ctx);
		ioAdaptor.sessionError(cause, sess);
	}
	
	
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		Session sess = getSession(ctx);
		ioAdaptor.sessionActive(sess);
	}  
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Session sess = getSession(ctx);
		ioAdaptor.sessionInactive(sess);
	} 
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			Session sess = getSession(ctx);
			ioAdaptor.sessionIdle(sess);
        }
	}
	
	private Session attachSession(ChannelHandlerContext ctx){
		Session sess = new TcpSession(ctx); 
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
