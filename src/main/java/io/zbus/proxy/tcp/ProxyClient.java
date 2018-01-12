package io.zbus.proxy.tcp;

import java.io.Closeable;
import java.io.IOException;
import java.util.Queue;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.transport.EventLoop;
import io.zbus.transport.IoAdaptor;
import io.zbus.transport.Session;
import io.zbus.transport.tcp.NettyAdaptor;

class ProxyClient implements IoAdaptor, Closeable {
	static final Logger log = LoggerFactory.getLogger(ProxyClient.class);
	Bootstrap bootstrap;
	Session upstream;
	Session downstream;
	int connectTimeout;
	int readTimeout; 
	 
	ProxyClient(Session upstream, final Proxy proxy) { 
		this.upstream = upstream;
		this.bootstrap = new Bootstrap(); 
		
		EventLoop loop = proxy.server.getEventLoop();
		this.connectTimeout = proxy.connectTimeout;
		this.readTimeout = proxy.idleTimeout;
		
		
		final SslContext sslCtx = loop.getSslContext(); 
		bootstrap.group(loop.getGroup()) 
		 .channel(NioSocketChannel.class)  
		 .handler(new ChannelInitializer<SocketChannel>() { 
			NettyAdaptor nettyToIoAdaptor = new NettyAdaptor(ProxyClient.this);
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {  
				ChannelPipeline p = ch.pipeline();
				if(sslCtx != null){
					p.addLast(sslCtx.newHandler(ch.alloc()));
				} 
				p.addLast(nettyToIoAdaptor);
			}
		});  
		 
		final ChannelFuture connFuture = bootstrap.connect(proxy.targetHost, proxy.targetPort); 
		loop.getGroup().submit(new Runnable() {
			
			@Override
			public void run() { 
				try {
					connFuture.sync().await(ProxyClient.this.connectTimeout);
					if(connFuture.channel().isActive()) return;
					log.warn("Connection to target timeout");
					try {
						ProxyClient.this.upstream.close();
					} catch (IOException ex) {
						//ignore
					}
				} catch (InterruptedException e) { 
					log.error(e.getMessage(), e);
				} catch (Exception e) { 
					try {
						ProxyClient.this.upstream.close();
					} catch (IOException ex) {
						//ignore
					}
				} 
			}
		});
	} 
	
	@Override
	public void sessionCreated(Session sess) throws IOException { 
		Queue<Object> delayed = upstream.attr("delayed");
		if(delayed != null){
			while(true){
				Object msg = delayed.poll();
				if(msg == null) break;
				sess.write(msg);
			}
		}  
		this.downstream = sess;
		upstream.attr("down", downstream);
		downstream.attr("up", upstream);
	}

	@Override
	public void sessionToDestroy(Session sess) throws IOException { 
		cleanSession(sess);
	}

	@Override
	public void onMessage(Object msg, Session sess) throws IOException { 
		upstream.write(msg);
	}

	@Override
	public void onError(Throwable e, Session sess) throws Exception { 
		cleanSession(sess);
	}

	@Override
	public void onIdle(Session sess) throws IOException { 
		cleanSession(sess);
	} 
	
	private void cleanSession(Session sess){
		try {
			upstream.close();
			close();
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}
	
	@Override
	public void close() throws IOException {
		if(this.downstream != null){
			this.downstream.close();
		} 
	}
}