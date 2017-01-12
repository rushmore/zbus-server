package io.zbus.net.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import io.zbus.net.CodecInitializer;
import io.zbus.net.IoAdaptor;
import io.zbus.net.EventDriver;
import io.zbus.net.Server;
import io.zbus.util.logger.Logger;
import io.zbus.util.logger.LoggerFactory;

public class TcpServer implements Server {
	private static final Logger log = LoggerFactory.getLogger(TcpServer.class); 
	
	protected CodecInitializer codecInitializer; 
	protected EventDriver eventDriver;
	protected boolean ownEventDriver; 
	//Port ==> Server IoAdaptor
	protected Map<Integer, ServerInfo> serverMap = new ConcurrentHashMap<Integer, ServerInfo>();
  
	public TcpServer(){
		this(null); 
	}
	
	public TcpServer(EventDriver driver){ 
		this.eventDriver = driver; 
		if (this.eventDriver == null) {
			this.eventDriver = new EventDriver();
			this.ownEventDriver = true;
		} else {
			this.ownEventDriver = false;
		} 
	} 
	
	@Override
	public void close() throws IOException {
		if(ownEventDriver && eventDriver != null){
			eventDriver.close();
			eventDriver = null;
		}
	} 
	 
	public void join() throws InterruptedException {
		for(Entry<Integer, ServerInfo> e : serverMap.entrySet()){
			ChannelFuture cf = e.getValue().serverChanneFuture;
			cf.sync().channel().closeFuture().sync();
		}
	}

	@Override
	public void start(int port, IoAdaptor ioAdaptor) {
		start("0.0.0.0", port, ioAdaptor);
	}

	@Override
	public void start(final String host, final int port, IoAdaptor ioAdaptor) {
		EventLoopGroup bossGroup = (EventLoopGroup)eventDriver.getGroup();
		EventLoopGroup workerGroup = (EventLoopGroup)eventDriver.getWorkerGroup(); 
		if(workerGroup == null){
			workerGroup = bossGroup;
		} 
		
		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup)
		 .option(ChannelOption.SO_BACKLOG, 10240)
		 .channel(NioServerSocketChannel.class) 
		 .handler(new LoggingHandler(LogLevel.INFO))
		 .childHandler(new MyChannelInitializer(ioAdaptor));
		
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
		serverMap.put(port, info);
	} 
	
	public int getRealPort(int bindPort) throws InterruptedException{
		if(!serverMap.containsKey(bindPort)){
			return -1; //indicates not found;
		}
		ServerInfo e = serverMap.get(bindPort);
		SocketAddress addr = e.serverChanneFuture.await().channel().localAddress();
		return ((InetSocketAddress)addr).getPort();
	}
	
	public EventDriver getEventDriver() {
		return this.eventDriver;
	}
	
	public void codec(CodecInitializer codecInitializer) {
		this.codecInitializer = codecInitializer;
	} 
	
	static class ServerInfo{
		ServerBootstrap bootstrap;
		ChannelFuture serverChanneFuture;
	}  
	
	class MyChannelInitializer extends ChannelInitializer<SocketChannel>{ 
		private NettyToIoAdaptor nettyToIoAdaptor;
		private CodecInitializer codecInitializer;
		
		public MyChannelInitializer(IoAdaptor ioAdaptor){
			this(ioAdaptor, null);
		}
		public MyChannelInitializer(IoAdaptor ioAdaptor, CodecInitializer codecInitializer){ 
			this.nettyToIoAdaptor = new NettyToIoAdaptor(ioAdaptor);
			this.codecInitializer = codecInitializer;
		}
		
		private CodecInitializer getCodecInitializer(){
			if(this.codecInitializer != null) return this.codecInitializer;
			return TcpServer.this.codecInitializer;
		}
		
		@Override
		protected void initChannel(SocketChannel ch) throws Exception {  
			ChannelPipeline p = ch.pipeline(); 
			int timeout = eventDriver.getIdleTimeInSeconds();
			p.addLast(new IdleStateHandler(0, 0, timeout));
			SslContext sslCtx = eventDriver.getSslContext();
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
