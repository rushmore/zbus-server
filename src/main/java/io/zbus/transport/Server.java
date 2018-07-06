package io.zbus.transport;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.zbus.kit.NetKit; 

/**
 * Server capable of start multiple ServerAdaptor in different ports.
 * 
 * <code>IoAdaptor</code> is the main battle field to start, it provides feature extension points to plugin
 * business logic.
 * 
 * When SSL is required, start with an extra parameter SslContext which can be built from <code>Ssl</code>
 * 
 * @author leiming.hong Jun 27, 2018
 *
 */
public class Server implements Closeable {
	
	public static interface CodecInitializer {
		void initPipeline(List<ChannelHandler> p);
	}

	private static final Logger logger = LoggerFactory.getLogger(Server.class); 
	
	protected CodecInitializer codecInitializer; 
	
	protected EventLoopGroup bossGroup;     
	protected EventLoopGroup workGroup;    
	
	protected int idleTimeInSeconds = 180; //180s 
	protected int packageSizeLimit = 1024*1024*1024; //maximum of 1G  
	protected int maxSocketCount = 102400;
	
	//Port ==> Server IoAdaptor
	protected Map<Integer, ServerInfo> listenTable = new ConcurrentHashMap<Integer, ServerInfo>();
  
	public Server(){
		this.bossGroup = new NioEventLoopGroup(1);
		this.workGroup = new NioEventLoopGroup();
	}  
	
	public void start(int port, IoAdaptor ioAdaptor) {
		start(port, ioAdaptor, null);
	}
 
	public void start(int port, IoAdaptor ioAdaptor, SslContext sslContext) {
		start("0.0.0.0:"+port, ioAdaptor, sslContext);
	}
   
	public void start(final String address, IoAdaptor ioAdaptor) {
		start(address, ioAdaptor, null);
	}
	
	public void start(final String address, IoAdaptor ioAdaptor, SslContext sslContext) {  
		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workGroup)
		 .option(ChannelOption.SO_BACKLOG, maxSocketCount) 
		 .channel(NioServerSocketChannel.class) 
		 .handler(new LoggingHandler(LogLevel.DEBUG))
		 .childHandler(new SocketChannelInitializer(ioAdaptor, sslContext));
		
		Object[] bb = NetKit.hostPort(address);
		String host = (String)bb[0];
		int port = (int)bb[1];
		ServerInfo info = new ServerInfo(); 
		info.bootstrap = b;
		info.serverChanneFuture = b.bind(host, port).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()){
					if(sslContext != null){
						logger.info("Server([SSL]{}:{}) started", host, port);
					} else {
						logger.info("Server({}:{}) started", host, port);
					} 
				} else { 
					String message = String.format("Server(%s:%d) failed to start", host, port);
					throw new IOException(message, future.cause());
				}
			}
		}); 
		listenTable.put(port, info); 
	} 
	
	public boolean isStarted() {
		return !this.listenTable.isEmpty();
	}
	
	@Override
	public void close() throws IOException {
		if(bossGroup != null){
			bossGroup.shutdownGracefully();
			bossGroup = null;
		}
		if(workGroup != null){
			workGroup.shutdownGracefully();
			workGroup = null;
		}
	} 
	 
	public int getRealPort(int bindPort) throws InterruptedException{
		if(!listenTable.containsKey(bindPort)){
			return -1; //indicates not found;
		}
		ServerInfo e = listenTable.get(bindPort);
		SocketAddress addr = e.serverChanneFuture.await().channel().localAddress();
		return ((InetSocketAddress)addr).getPort();
	}
	  
	 
	public void codec(CodecInitializer codecInitializer) {
		this.codecInitializer = codecInitializer;
	}  
	
	static class ServerInfo{
		ServerBootstrap bootstrap;
		ChannelFuture serverChanneFuture;
	}  
	
	class SocketChannelInitializer extends ChannelInitializer<SocketChannel>{ 
		private NettyAdaptor nettyToIoAdaptor;
		private CodecInitializer codecInitializer;
		private SslContext sslContext;
		
		public SocketChannelInitializer(IoAdaptor ioAdaptor, SslContext sslContext){
			this.nettyToIoAdaptor = new NettyAdaptor(ioAdaptor);
			this.sslContext = sslContext;
		} 
		
		private CodecInitializer getCodecInitializer(){
			if(this.codecInitializer != null) return this.codecInitializer;
			return Server.this.codecInitializer;
		}
		
		@Override
		protected void initChannel(SocketChannel ch) throws Exception {  
			ChannelPipeline p = ch.pipeline();  
			p.addLast(new IdleStateHandler(0, 0, idleTimeInSeconds)); 
			if(sslContext != null){
				p.addLast(sslContext.newHandler(ch.alloc()));
			}
			CodecInitializer initializer = getCodecInitializer();
			if(initializer != null){
				List<ChannelHandler> handlers = new ArrayList<>();
				initializer.initPipeline(handlers);
				for(ChannelHandler handler : handlers){
					 p.addLast(handler); 
				}
			}	 
			p.addLast(this.nettyToIoAdaptor);
		} 
	} 
}


@Sharable
class NettyAdaptor extends ChannelInboundHandlerAdapter {
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
			throw new IllegalStateException("Missing sessionKey");
		}
		Session sess = sessionMap.get(attr.get()); 
		if(sess == null){
			throw new IllegalStateException("Session and ChannelHandlerContext mapping not found");
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

class AttributeMap {
	private ConcurrentMap<String, Object> attributes = null;
	
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
}
