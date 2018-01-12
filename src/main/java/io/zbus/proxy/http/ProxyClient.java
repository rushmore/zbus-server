package io.zbus.proxy.http;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.zbus.kit.ThreadKit.ManualResetEvent;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Message;
import io.zbus.mq.MqClient;
import io.zbus.transport.CodecInitializer;
import io.zbus.transport.EventLoop;
import io.zbus.transport.Session;
import io.zbus.transport.http.MessageCodec;
import io.zbus.transport.tcp.TcpClient; 

public class ProxyClient extends TcpClient<io.zbus.transport.http.Message, io.zbus.transport.http.Message> { 
	private static final Logger log = LoggerFactory.getLogger(ProxyClient.class); 
	private Map<String, Context> requestTable = new ConcurrentHashMap<String, Context>();  
	private boolean targetMessageIdentifiable = false;
	private MessageFilter recvFilter; 
	private int defaultTimeout = 10000;
	
	private Context context;
	private ManualResetEvent ready = new ManualResetEvent(true);
	
	private static class Context{
		String topic;
		String msgId;
		String sender;
		MqClient senderClient;
	}
	
	public ProxyClient(String address, final EventLoop loop) {  
		super(address, loop);     
		
		codec(new CodecInitializer() {
			@Override
			public void initPipeline(List<ChannelHandler> p) {
				p.add(new HttpRequestEncoder()); 
				p.add(new HttpResponseDecoder());  
				p.add(new HttpObjectAggregator(loop.getPackageSizeLimit()));
				p.add(new MessageCodec());
			}
		});  
		
		onDisconnected(new DisconnectedHandler() { 
			@Override
			public void onDisconnected() throws IOException {
				log.info("Disconnected from(%s) ID=%s", serverAddress(), clientId);
				ProxyClient.this.close();
			}
		}); 
		
		onError(new ErrorHandler() { 
			@Override
			public void onError(Throwable e, Session session) throws IOException {
				ProxyClient.this.close();
			}
		});
	}
	
	@Override
	public void onMessage(Object msg, Session sess) throws IOException {
		Message res = new Message((io.zbus.transport.http.Message)msg);  
		
		String msgId = res.getId();  
		Context ctx = null;
		if(targetMessageIdentifiable){ 
			ctx = requestTable.remove(msgId); 
		} else {  
			ctx = context;
			context = null; //clear  
		}  
		
		if(ctx == null){  
			log.warn("Message from target without context: " + res);
			return; //ignore
		}
		
		res.setId(ctx.msgId);
		res.setTopic(ctx.topic);
		res.setReceiver(ctx.sender);  
		if(!targetMessageIdentifiable) {
			ready.set();
		}
		
		if(recvFilter != null){
			if( recvFilter.filter(res, ctx.senderClient) == false){
				return;
			}
		}  
		try {
			ctx.senderClient.route(res);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}  
	} 
	
	public void sendMessage(Message msg, MqClient senderClient, int timeoutMs) 
			throws IOException, InterruptedException{  
		if(targetMessageIdentifiable){ 
			sendMessageUnsafe(msg, senderClient, timeoutMs);
			return;
		}
		
		synchronized (ready) { 
			sendMessageUnsafe(msg, senderClient, timeoutMs); 
			ready.await(timeoutMs, TimeUnit.MILLISECONDS);
			if(!ready.isSignalled()){
				throw new IOException("waiting result timeout, should close connection");
			}    
		}   
	}
	
	private void sendMessageUnsafe(Message msg, MqClient senderClient, int timeoutMs) 
			throws IOException, InterruptedException{   
		
		Context ctx = new Context();
		ctx.msgId = msg.getId();
		ctx.topic = msg.getTopic();
		ctx.sender = msg.getSender();
		ctx.senderClient = senderClient; 
		
		if(targetMessageIdentifiable){
			requestTable.put(ctx.msgId, ctx); 
		} else {
			context = ctx; 
			ready.reset(); 
		} 
		super.sendMessage(msg);   
	}
	
	public void sendMessage(Message msg, MqClient senderClient) 
			throws IOException, InterruptedException{
		sendMessage(msg, senderClient, defaultTimeout); //default to 
	}  

	public MessageFilter getRecvFilter() {
		return recvFilter;
	}

	public void setRecvFilter(MessageFilter recvFilter) {
		this.recvFilter = recvFilter;
	}

	public int getDefaultTimeout() {
		return defaultTimeout;
	}

	public void setDefaultTimeout(int defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	}

	public void setTargetMessageIdentifiable(boolean targetMessageIdentifiable) {
		this.targetMessageIdentifiable = targetMessageIdentifiable;
	}  
}  
