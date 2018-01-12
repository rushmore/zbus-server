package io.zbus.proxy.tcp;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.zbus.kit.ConfigKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.proxy.tcp.TcpProxyConfig.ProxyEntry;
import io.zbus.transport.EventLoop;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session;
import io.zbus.transport.tcp.TcpServer;

public class TcpProxy implements Closeable {
	List<Proxy> proxyList = new ArrayList<Proxy>();
	EventLoop loop;
	
	public TcpProxy(TcpProxyConfig config){
		if(config.getEntries() == null) return;
		init(config.getEntries());
	}
	
	
	public TcpProxy(int proxyPort, String targetAddress) { 
		ProxyEntry entry = new ProxyEntry();
		entry.targetAddress = targetAddress;
		entry.proxyPort = proxyPort;
		
		init(Arrays.asList(entry));
	}
	
	public void init(List<ProxyEntry> entries){
		this.loop = new EventLoop();
		for(ProxyEntry entry : entries){
			Proxy proxy = new Proxy(entry, loop);
			proxyList.add(proxy);
		}
	}
	
	public void start(){
		for(Proxy proxy : proxyList){
			proxy.start();
		}
	}
	
	@Override
	public void close() throws IOException { 
		if(loop != null){
			loop.close();
		}
		for(Proxy proxy : proxyList){
			proxy.close();
		}
	}
	
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		String configFile = ConfigKit.option(args, "-conf", "conf/tcp_proxy.xml"); 
		TcpProxyConfig config = new TcpProxyConfig();
		config.loadFromXml(configFile);  
		TcpProxy proxy = new TcpProxy(config);
		proxy.start();
	} 
	
}

class Proxy extends ServerAdaptor implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(TcpProxy.class);  
	String proxyHost = "0.0.0.0";
	int proxyPort; 
	
	String targetHost;
	int targetPort = 80;
	
	int connectTimeout = 3000;  
	int idleTimeout = 60000; 
	
	TcpServer server;    
	EventLoop loop;
	
	public Proxy(ProxyEntry entry, EventLoop loop){
		this.proxyHost = entry.getProxyHost();
		this.proxyPort = entry.getProxyPort();
		
		String[] bb = entry.getTargetAddress().split("[:]");
		if(bb.length > 0){
			this.targetHost = bb[0].trim();
		}
		if(bb.length > 1){
			this.targetPort = Integer.valueOf(bb[1].trim());
		}  
		this.connectTimeout = entry.getConnectTimeout();
		this.idleTimeout = entry.getIdleTimeout();
		this.loop = loop.duplicate();
		loop.setIdleTimeInSeconds(this.idleTimeout/1000);
	}
	
	public Proxy(int proxyPort, String targetAddress) { 
		this.proxyPort = proxyPort;
		String[] bb = targetAddress.split("[:]");
		if(bb.length > 0){
			this.targetHost = bb[0].trim();
		}
		if(bb.length > 1){
			this.targetPort = Integer.valueOf(bb[1].trim());
		} 
	}
	
	@Override
	public void sessionCreated(Session sess) throws IOException { 
		super.sessionCreated(sess); //add to session table
		
		ProxyClient client = new ProxyClient(sess, this); 
		sess.attr("downClient", client);
	} 
	 
	
	protected void cleanSession(Session sess){
		try { 
			ProxyClient client = sess.attr("downClient");
			if(client != null){
				client.close();
			}
			super.cleanSession(sess);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}
	
	@Override
	public void onMessage(Object msg, Session sess) throws IOException {
		Session down = sess.attr("down");
		if(down == null){
			Queue<Object> delayed = sess.attr("delayed");
			if(delayed == null){
				delayed = new ConcurrentLinkedQueue<Object>();
			}
			sess.attr("delayed", delayed);
			delayed.add(msg);
		} else {
			down.write(msg);
		}
	} 
	
	@Override
	public void close() throws IOException { 
		server.close(); 
		loop.close();
	}
	
	public void start(){
		server = new TcpServer(loop);   
		server.start(proxyPort, this);
	} 
}
