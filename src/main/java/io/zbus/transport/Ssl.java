package io.zbus.transport;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.zbus.kit.FileKit;

/**
 * SSL helper methods for creating <code>SslContext</code>, such as from public/private key pairs.
 * 
 * @author leiming.hong Jun 27, 2018
 *
 */
public class Ssl {
	private static final Logger log = LoggerFactory.getLogger(Ssl.class);
	private static Map<String, SslContext> sslContextCache = new ConcurrentHashMap<>();
	
	public static SSLEngine buildSSLEngine(String host, int port, ByteBufAllocator alloc){
		String key = String.format("%s:%d", host,port);
		SslContext sslContext = sslContextCache.get(key); 
		if(sslContext == null){
			sslContext = buildSslContext();
			sslContextCache.put(key, sslContext);
		}
		
		SSLEngine sslEngine = sslContext.newEngine(alloc, host, port); 
		sslEngine.setUseClientMode(true);
		SSLParameters params = sslEngine.getSSLParameters();
		params.setEndpointIdentificationAlgorithm("HTTPS");
		sslEngine.setSSLParameters(params);
		return sslEngine; 
	}
	
	private static SslContext buildSslContext() { 
		try {
			SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
					.sslProvider(SslProvider.JDK)
					.sessionCacheSize(0)
					.sessionTimeout(0);
			String[] protocols = new String[] { "TLSv1.2", "TLSv1.1", "TLSv1" };
			sslContextBuilder.protocols(protocols);
			SslContext sslContext = sslContextBuilder.build();
			return sslContext;
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	} 
	
	public static SslContext buildClientSsl(String certFileContent){ 
		if(certFileContent == null) return null;
		
		InputStream certStream = new ByteArrayInputStream(certFileContent.getBytes());
		return buildClientSsl(certStream);
	}
	
	public static SslContext buildClientSsl(InputStream certStream){
		try { 
			SslContextBuilder builder = SslContextBuilder.forClient().trustManager(certStream);
			return builder.build();
		} catch (Exception e) { 
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}
	
	public static SslContext buildClientSslFromFile(String certFile){ 
		InputStream certStream = null;
		try {
			certStream = new FileInputStream(certFile);
			return buildClientSsl(certStream);  
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		} finally {
			if(certStream != null){
				try {
					certStream.close();
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			}
		} 
	}
	 
	public static SslContext buildServerSsl(InputStream certStream, InputStream privateKeyStream) { 
		try {
			SslContextBuilder builder = SslContextBuilder.forServer(certStream, privateKeyStream);
			return builder.build(); 
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}
	
	public static SslContext buildServerSsl(String certStreamPath, String privateKeyPath) { 
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
		
		SslContext context = buildServerSsl(certStream, privateKeyStream);
		
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
		return context;
	}
	 

	public static SslContext buildServerSslOfSelfSigned() { 
		try {
			SelfSignedCertificate cert = new SelfSignedCertificate(); 
			InputStream certStream = new FileInputStream(cert.certificate());
			InputStream privateKeyStream = new FileInputStream(cert.privateKey());
			SslContext context = buildServerSsl(certStream, privateKeyStream);
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
			
			return context;
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		} 
	}
	
	public static SslContext buildClientSslOfSelfSigned() { 
		try {
			SelfSignedCertificate cert = new SelfSignedCertificate(); 
			InputStream certStream = new FileInputStream(cert.certificate()); 
			SslContext context = buildClientSsl(certStream);
			
			try{
				certStream.close();
			} catch(IOException e) {
				//ignore
			}
			return context;
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}  
}
