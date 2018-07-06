package io.zbus.transport.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.JsonKit;
import io.zbus.transport.AbastractClient;
import io.zbus.transport.DataHandler;
import io.zbus.transport.Message;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * 
 * Client of Websocket, via OkHttp3.
 * 
 * @author leiming.hong Jun 27, 2018
 *
 */
public class WebsocketClient extends AbastractClient {
	private static final Logger logger = LoggerFactory.getLogger(WebsocketClient.class);   
	
	public DataHandler<String> onText;
	public DataHandler<ByteBuffer> onBinary; 
	
	public long lastActiveTime = System.currentTimeMillis(); 
	
	private OkHttpClient client; 
	private String address;
	private WebSocket ws;
	private Object wsLock = new Object();
	
	private List<String> cachedSendingMessages = new ArrayList<String>();  
	
	public WebsocketClient(String address, OkHttpClient client) { 
		super();
		
		this.client = client;
		if(!address.startsWith("ws://") && !address.startsWith("wss://")) {
			address = "ws://" + address;
		}
		this.address = address; 
		
		onText = msg-> {  
			Message response = JsonKit.parseObject(msg, Message.class); 
			if(onMessage != null) {
				onMessage.handle(response);
			} 
		}; 
		
		//TODO onBinary
		
		onClose = ()-> {
			synchronized (wsLock) {
				if(ws != null){
					ws.close(1000, null); 
					ws = null;
				}
			}; 
			
			try {
				logger.info("Trying to reconnect " + WebsocketClient.this.address);
				Thread.sleep(reconnectDelay);
			} catch (InterruptedException e) {
				// ignore
			}  
			connect();
		};
		
		onError = e -> {;
			if(onClose != null){
				try {
					onClose.handle();
				} catch (Exception ex) {
					logger.error(ex.getMessage(), ex);
				}
			}
		};
	}
	
	public WebsocketClient(String address){
		this(address, new OkHttpClient());  
	} 
	
	@Override
	protected void sendMessage0(Message data) {
		sendMessage(JsonKit.toJSONString(data));
	}  
	
	@Override
	public void close() throws IOException { 
		super.close();
		
		synchronized (this.wsLock) {
			if(this.ws != null){
				this.ws.close(1000, null);  
				this.ws = null;
			} 
		} 
	} 
	
	public void sendMessage(String command){
		synchronized (wsLock) {
			if(this.ws == null){
				this.cachedSendingMessages.add(command);
				this.connect();
				return;
			} 
			this.ws.send(command);
		}  
	}  
	
	public synchronized void connect(){    
		connectUnsafe();
	}
	
	protected void connectUnsafe(){   
		lastActiveTime = System.currentTimeMillis(); 
		Request request = new Request.Builder()
				.url(address)
				.build(); 
		
		this.ws = client.newWebSocket(request, new WebSocketListener() {
			@Override
			public void onOpen(WebSocket webSocket, Response response) {
				String msg = String.format("Websocket(%s) connected", address);
				logger.info(msg);
				response.close();
				
				if(cachedSendingMessages.size()>0){
					for(String json : cachedSendingMessages){
						sendMessage(json);
					}
					cachedSendingMessages.clear();
				} 
				if(onOpen != null){
					runner.submit(()->{
						try {
							onOpen.handle();
						} catch (Exception e) {
							logger.error(e.getMessage(), e);
						}
					});
				}
			}

			@Override
			public void onMessage(WebSocket webSocket, String text) {
				lastActiveTime = System.currentTimeMillis(); 
				try {
					if(onText != null){
						onText.handle(text);
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
			
			@Override
			public void onMessage(WebSocket webSocket, ByteString bytes) {
				lastActiveTime = System.currentTimeMillis();
				try {
					if(onBinary != null){
						onBinary.handle(bytes.asByteBuffer());
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}

			@Override
			public void onClosed(WebSocket webSocket, int code, String reason) { 
				String msg = String.format("Websocket(%s) closed", address);
				logger.info(msg);
				if(onClose != null){
					try {
						onClose.handle();
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				}
			}

			@Override
			public void onFailure(WebSocket webSocket, Throwable t, Response response) { 
				String error = String.format("Websocket(%s) error: %s", address, t.getMessage());
				logger.error(error);
				if(response != null) {
					response.close();
				}
				if(onError != null){
					onError.handle(t);
				}
			} 
		}); 
	}  
}
