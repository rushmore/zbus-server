package io.zbus.transport.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.transport.CodecInitializer;
import io.zbus.transport.EventLoop;
import io.zbus.transport.IoAdaptor;
import io.zbus.transport.Server;
import io.zbus.transport.ServerAddress;

public class TcpServer implements Server {
	private static final Logger log = LoggerFactory.getLogger(TcpServer.class); 
	
	protected CodecInitializer codecInitializer; 
	protected EventLoop loop;
	protected boolean ownLoop; 
	protected IoAdaptor defaultIoAdaptor;
	protected ServerAddress defaultServerAddress;
	//Port ==> Server IoAdaptor
	protected Map<Integer, ServerInfo> listenTable = new ConcurrentHashMap<Integer, ServerInfo>();
  
	public TcpServer(){
		this(null); 
	}
	
	public TcpServer(EventLoop loop){ 
		this.loop = loop; 
		if (this.loop == null) {
			this.loop = new EventLoop();
			this.ownLoop = true;
		} else {
			this.ownLoop = false;
		} 
	}  
	

	@Override
	public void start(int port, IoAdaptor ioAdaptor) {
		start("0.0.0.0", port, ioAdaptor, true);
	}

	@Override
	public void start(final String host, final int port, IoAdaptor ioAdaptor){
		start(host, port, ioAdaptor, true);
	} 
	
	public void start(final String host, final int port, IoAdaptor ioAdaptor, boolean isDefault) {
		EventLoopGroup bossGroup = (EventLoopGroup)loop.getGroup();
		EventLoopGroup workerGroup = (EventLoopGroup)loop.getWorkerGroup(); 
		if(workerGroup == null){
			workerGroup = bossGroup;
		} 
		
		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup)
		 .option(ChannelOption.SO_BACKLOG, 102400) //TODO make it configurable
		 .channel(NioServerSocketChannel.class) 
		 .handler(new LoggingHandler(LogLevel.DEBUG))
		 .childHandler(new SocketChannelInitializer(ioAdaptor));
		
		ServerInfo info = new ServerInfo(); 
		info.bootstrap = b;
		info.serverChanneFuture = b.bind(host, port).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()){
					log.info("Server(%s:%d) started", host, port);
				} else { 
					String message = String.format("Server(%s:%d) failed to start", host, port);
					throw new IOException(message, future.cause());
				}
			}
		}); 
		listenTable.put(port, info);
		
		if(isDefault){
			this.defaultIoAdaptor = ioAdaptor;
			this.defaultServerAddress = new ServerAddress(host+":"+port, loop.isSslEnabled());
		}
	} 
	
	@Override
	public void close() throws IOException {
		if(ownLoop && loop != null){
			loop.close(); 
			loop = null;
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
	
	@Override
	public EventLoop getEventLoop() {
		return this.loop;
	}
	
	@Override
	public void codec(CodecInitializer codecInitializer) {
		this.codecInitializer = codecInitializer;
	} 
	
	@Override
	public IoAdaptor getIoAdatpr() { 
		return this.defaultIoAdaptor;
	}
	
	@Override
	public ServerAddress getServerAddress() {
		return defaultServerAddress;
	}
	
	static class ServerInfo{
		ServerBootstrap bootstrap;
		ChannelFuture serverChanneFuture;
	}  
	
	class SocketChannelInitializer extends ChannelInitializer<SocketChannel>{ 
		private NettyAdaptor nettyToIoAdaptor;
		private CodecInitializer codecInitializer;
		
		public SocketChannelInitializer(IoAdaptor ioAdaptor){
			this(ioAdaptor, null);
		}
		public SocketChannelInitializer(IoAdaptor ioAdaptor, CodecInitializer codecInitializer){ 
			this.nettyToIoAdaptor = new NettyAdaptor(ioAdaptor);
			this.codecInitializer = codecInitializer;
		}
		
		private CodecInitializer getCodecInitializer(){
			if(this.codecInitializer != null) return this.codecInitializer;
			return TcpServer.this.codecInitializer;
		}
		
		@Override
		protected void initChannel(SocketChannel ch) throws Exception {  
			ChannelPipeline p = ch.pipeline(); 
			int timeout = loop.getIdleTimeInSeconds();
			p.addLast(new IdleStateHandler(0, 0, timeout));
			SslContext sslCtx = loop.getSslContext();
			if(sslCtx != null){
				p.addLast(sslCtx.newHandler(ch.alloc()));
			}
			CodecInitializer initializer = getCodecInitializer();
			if(initializer != null){
				List<ChannelHandler> handlers = new ArrayList<ChannelHandler>();
				initializer.initPipeline(handlers);
				for(ChannelHandler handler : handlers){
					 p.addLast((ChannelHandler)handler); 
				}
			}	 
			p.addLast(this.nettyToIoAdaptor);
		} 
	}
	 
}
