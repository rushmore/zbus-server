package io.zbus.transport.tcp;
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.zbus.kit.StrKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.transport.AbstractClient;
import io.zbus.transport.CodecInitializer;
import io.zbus.transport.EventLoop;
import io.zbus.transport.Id;
import io.zbus.transport.ServerAddress;
import io.zbus.transport.SslKit;


public class TcpClient<REQ extends Id, RES extends Id> extends AbstractClient<REQ, RES> {
	private static final Logger log = LoggerFactory.getLogger(TcpClient.class); 
	  
	protected final String host;
	protected final int port;   
	
	protected Bootstrap bootstrap;
	protected final EventLoopGroup group;  
	protected SslContext sslCtx; 
	protected ChannelFuture channelFuture; 
	protected CodecInitializer codecInitializer;   
	
	protected volatile ScheduledExecutorService heartbeator = null; 
	protected HeartbeatMessageBuilder<REQ> heartbeatMessageBuilder;

	
	public TcpClient(String address, EventLoop loop){   
		group = loop.getGroup(); 
		
		Object[] hp = StrKit.hostPort(address);
		this.host = (String)hp[0];
		this.port = (Integer)hp[1]; 
	} 
	
	public TcpClient(ServerAddress serverAddress, EventLoop loop){   
		this.group = loop.getGroup();
		
		Object[] hp = StrKit.hostPort(serverAddress.address);
		this.host = (String)hp[0];
		this.port = (Integer)hp[1]; 
		
		if(serverAddress.isSslEnabled()){
			if(serverAddress.getCertificate() == null){
				throw new IllegalArgumentException("SSL enabled, but missing certificate content");
			}
			try{
				this.sslCtx = SslKit.buildClientSsl(serverAddress.certificate);
			} catch (Exception e) {
				throw new IllegalStateException("SSL enabled, but SSL context creation failed", e); 
			}  
		} 
	}  
	  
	protected String serverAddress(){
		return String.format("%s%s:%d", sslCtx==null? "" : "[SSL]", host, port);
	}
	
	public void codec(CodecInitializer codecInitializer) {
		this.codecInitializer = codecInitializer;
	}  

	public synchronized void connectAsync(){  
		init(); 
		activeLatch = new CountDownLatch(1);
		channelFuture = bootstrap.connect(host, port);
	}   
	
	
	public void connectSync(long timeout) throws IOException, InterruptedException {
		if(hasConnected()) return; 
		
		synchronized (this) {
			if(!hasConnected()){ 
	    		connectAsync();
	    		activeLatch.await(timeout,TimeUnit.MILLISECONDS); 
				
	    		if(hasConnected()){ 
					return;
				}   
				channelFuture.sync();
				String msg = String.format("Connection(%s) failed", serverAddress()); 
				log.warn(msg);
				cleanSession();  
			}
		} 
	} 
	
	private void init(){
		if(bootstrap != null) return;
		if(this.group == null){
			throw new IllegalStateException("group missing");
		}
		bootstrap = new Bootstrap();
		bootstrap.group(this.group) 
		 .channel(NioSocketChannel.class)  
		 .handler(new ChannelInitializer<SocketChannel>() { 
			NettyAdaptor nettyToIoAdaptor = new NettyAdaptor(TcpClient.this);
			@Override
			protected void initChannel(SocketChannel ch) throws Exception { 
				if(codecInitializer == null){
					log.warn("Missing codecInitializer"); 
				} 
				ChannelPipeline p = ch.pipeline();
				if(sslCtx != null){
					p.addLast(sslCtx.newHandler(ch.alloc()));
				}
				if(codecInitializer != null){
					List<ChannelHandler> handlers = new ArrayList<ChannelHandler>();
					codecInitializer.initPipeline(handlers);
					for(ChannelHandler handler : handlers){ 
						p.addLast((ChannelHandler)handler);
					}
				}
				p.addLast(nettyToIoAdaptor);
			}
		});  
	}   
	
	@Override
	public void close() throws IOException {
		super.close();
		
		if(heartbeator != null){
			heartbeator.shutdownNow();
			heartbeator = null;
		} 
	}   
	
	public synchronized void startHeartbeat(int intervalInMillis, HeartbeatMessageBuilder<REQ> builder){
		this.heartbeatMessageBuilder = builder;
		if(heartbeator == null){
			heartbeator = Executors.newSingleThreadScheduledExecutor();
			this.heartbeator.scheduleAtFixedRate(new Runnable() {
				public void run() {
					try {
						if(heartbeatMessageBuilder != null){
							REQ msg = heartbeatMessageBuilder.build();
							invokeAsync(msg, null);
						}
					} catch (Exception e) {
						log.warn(e.getMessage(), e);
					}
				}
			}, intervalInMillis, intervalInMillis, TimeUnit.MILLISECONDS);
		}
	}    
	
	public interface HeartbeatMessageBuilder<REQ>{
		REQ build();
	} 
}
