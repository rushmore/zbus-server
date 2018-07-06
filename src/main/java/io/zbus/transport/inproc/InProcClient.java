package io.zbus.transport.inproc;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.StrKit;
import io.zbus.transport.AbastractClient;
import io.zbus.transport.IoAdaptor;
import io.zbus.transport.Message;
import io.zbus.transport.Session;

/**
 * 
 * Client of In-Process, optimized for speed. 
 * 
 * Crucial ideas:
 * <p>1) InprocClient is a Session type, can be plugged into <code>IoAdaptor</code> from server, which is event driven.
 * <p>2) write message in process means directly invoking onMessage of the client.
 * <p>3) send message to peer means directly invoking server's <code>IoAdaptor</code>'s onMessage
 * 
 * @author leiming.hong Jun 27, 2018
 *
 */
public class InprocClient extends AbastractClient implements Session { 
	private static final Logger logger = LoggerFactory.getLogger(InprocClient.class); 
	private ConcurrentMap<String, Object> attributes = null;
	
	private IoAdaptor ioAdaptor;
	private final String id = StrKit.uuid();
	private boolean active = false;
	private Object lock = new Object();
	
	public InprocClient(IoAdaptor ioAdaptor) {
		this.ioAdaptor = ioAdaptor; 
		
	}
	
	@Override
	public void connect() { 
		synchronized (lock) {
			if(active) return;
		}
		active = true;
		try {
			ioAdaptor.sessionCreated(this);
			if(onOpen != null) {
				runner.submit(()->{
					try {
						onOpen.handle();
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				});
			} 
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	@Override
	public void close() throws IOException { 
		super.close(); 
		
		active = false;
		ioAdaptor.sessionToDestroy(this); 
	} 

	@Override
	public String id() { 
		return id;
	}

	@Override
	public String remoteAddress() { 
		return "InprocServer";
	}

	@Override
	public String localAddress() {
		return "Inproc-"+id;
	}
	
 
	@Override
	public void write(Object msg) {  //Session received message  
		try {
			if(!(msg instanceof Message)) {
				logger.error("Message type required");
				return;
			}
			Message data = (Message)msg;  
			if(onMessage != null) {
				onMessage.handle(data);
			} 
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	@Override
	protected void sendMessage0(Message data) {  //Session send out message
		synchronized (lock) {
			if(!active) {
				connect();
			}
		}
		
		try {
			ioAdaptor.onMessage(data, this);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public boolean active() { 
		return active;
	}  
	
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
	
	@Override
	public String toString() { 
		return localAddress();
	}
}
