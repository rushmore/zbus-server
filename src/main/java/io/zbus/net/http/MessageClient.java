package io.zbus.net.http;

import java.io.IOException;
import java.util.List;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.zbus.net.CodecInitializer;
import io.zbus.net.IoDriver;
import io.zbus.net.http.Message.MessageInvoker;
import io.zbus.net.tcp.TcpClient;

public class MessageClient extends TcpClient<Message, Message> implements MessageInvoker{
	
	public MessageClient(String address, final IoDriver driver){
		super(address, driver); 
		
		codec(new CodecInitializer() {
			@Override
			public void initPipeline(List<ChannelHandler> p) {
				p.add(new HttpRequestEncoder()); 
				p.add(new HttpResponseDecoder());  
				p.add(new HttpObjectAggregator(driver.getPackageSizeLimit()));
				p.add(new MessageToHttpWsCodec());
			}
		});
		
		startHeartbeat(120);//sending heartbeat every 2 minutes
	}
	
	@Override
	public void heartbeat() {
		if(this.hasConnected()){
			Message hbt = new Message();
			hbt.setCmd(Message.HEARTBEAT);
			try {
				this.invokeAsync(hbt, null);
			} catch (IOException e) {  
				//ignore
			}
		}
	} 
	 
	@Override
	public String toString() {
		return "MessageClient" +  super.toString();
	}
	
}
 
