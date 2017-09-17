package io.zbus.transport;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.zbus.kit.FileKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;

public class SslKit {
	private static final Logger log = LoggerFactory.getLogger(SslKit.class);
 
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
