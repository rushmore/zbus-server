package io.zbus.proxy.tcp;

import java.io.Closeable;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.zbus.kit.ConfigKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.transport.EventLoop;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session;
import io.zbus.transport.tcp.TcpServer;

public class TcpProxy extends ServerAdaptor implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(TcpProxy.class);  
	String proxyHost = "0.0.0.0";
	int proxyPort; 
	
	String targetHost;
	int targetPort = 80;
	
	int connectTimeout = 3000;  
	int idleTimeout = 60000; 
	
	TcpServer server;    
	EventLoop loop;
	
	public TcpProxy(ProxyConfig config){
		this.proxyHost = config.getProxyHost();
		this.proxyPort = config.getProxyPort();
		
		String[] bb = config.getTargetAddress().split("[:]");
		if(bb.length > 0){
			this.targetHost = bb[0].trim();
		}
		if(bb.length > 1){
			this.targetPort = Integer.valueOf(bb[1].trim());
		}  
		this.connectTimeout = config.getConnectTimeout();
		this.idleTimeout = config.getIdleTimeout();
		loop = new EventLoop();
		loop.setIdleTimeInSeconds(this.idleTimeout/1000);
	}
	
	public TcpProxy(int proxyPort, String targetAddress) { 
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
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		String configFile = ConfigKit.option(args, "-conf", "conf/tcp_proxy.xml"); 
		ProxyConfig config = new ProxyConfig();
		config.loadFromXml(configFile);  
		TcpProxy proxy = new TcpProxy(config);
		proxy.start();
	} 
}
