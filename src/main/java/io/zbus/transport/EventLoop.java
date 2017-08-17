package io.zbus.transport;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.zbus.kit.FileKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;

public class EventLoop implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(EventLoop.class);

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
	
	public void setServerSslContext(InputStream certStream, InputStream privateKeyStream) { 
		try {
			SslContextBuilder builder = SslContextBuilder.forServer(certStream, privateKeyStream);
			this.sslContext = builder.build(); 
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	public void setServerSslContext(String certStreamPath, String privateKeyPath) { 
		InputStream certStream = FileKit.inputStream(certStreamPath);
		if(certStream == null){
			throw new IllegalArgumentException("Certification file(" + certStreamPath + ") not exists");
		}
		InputStream privateKeyStream = FileKit.inputStream(privateKeyPath);
		if(privateKeyStream == null){
			try {
				certStream.close();
			} catch (IOException e) {
				//ignore
			}
			throw new IllegalArgumentException("PrivateKey file(" + privateKeyPath + ") not exists"); 
		}
		
		setServerSslContext(certStream, privateKeyStream);
		
		try {
			certStream.close();
		} catch (IOException e) {
			//ignore
		} 
		try {
			privateKeyStream.close();
		} catch (IOException e) {
			//ignore
		} 
	}
	
	public void setClientSslContext(InputStream certStream) { 
		try {
			SslContextBuilder builder = SslContextBuilder.forClient().trustManager(certStream);
			this.sslContext = builder.build();
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	public void setClientSslContext(String certStreamPath) { 
		InputStream certStream = FileKit.inputStream(certStreamPath);
		if(certStream == null){
			throw new IllegalArgumentException("Certification file(" +certStreamPath + ") not exists");
		}
		
		setClientSslContext(certStream);
		try {
			certStream.close();
		} catch (IOException e) {
			//ignore
		}
	}

	public void setServerSslContextOfSelfSigned() { 
		try {
			SelfSignedCertificate cert = new SelfSignedCertificate(); 
			InputStream certStream = new FileInputStream(cert.certificate());
			InputStream privateKeyStream = new FileInputStream(cert.privateKey());
			setServerSslContext(certStream, privateKeyStream);
			try{
				certStream.close();
			} catch(IOException e) {
				//ignore
			}
			try{
				privateKeyStream.close();
			} catch(IOException e) {
				//ignore
			}
			
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
	}
	
	public void setClientSslContextOfSelfSigned() { 
		try {
			SelfSignedCertificate cert = new SelfSignedCertificate(); 
			InputStream certStream = new FileInputStream(cert.certificate()); 
			setClientSslContext(certStream);
			
			try{
				certStream.close();
			} catch(IOException e) {
				//ignore
			}
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
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
