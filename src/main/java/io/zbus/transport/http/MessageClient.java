package io.zbus.transport.http;

import java.util.List;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.zbus.transport.CodecInitializer;
import io.zbus.transport.CompositeClient;
import io.zbus.transport.EventLoop;
import io.zbus.transport.IoAdaptor;
import io.zbus.transport.Server;
import io.zbus.transport.ServerAddress;
import io.zbus.transport.inproc.InProcClient;
import io.zbus.transport.tcp.TcpClient;
import io.zbus.transport.tcp.TcpClient.HeartbeatMessageBuilder;

public class MessageClient extends CompositeClient<Message, Message>{ 
	protected int heartbeatInterval = 60000; //60s
	
	public MessageClient(ServerAddress address, final EventLoop loop){
		initSupport(address, loop);
	}
	
	public MessageClient(String address, final EventLoop loop){ 
		ServerAddress serverAddress = new ServerAddress(address);
		initSupport(serverAddress, loop);
	} 
	
	public MessageClient(IoAdaptor serverIoAdaptor){ 
		support = new InProcClient<Message, Message>(serverIoAdaptor);
	} 
	
	public MessageClient(Server server){ 
		support = new InProcClient<Message, Message>(server.getIoAdaptor());
	}   
	
	protected void initSupport(ServerAddress address, final EventLoop loop){
		if(address.getServer() != null){
			support = new InProcClient<Message, Message>(address.getServer().getIoAdaptor());
			return;
		}
		
		TcpClient<Message, Message> tcp = new TcpClient<Message, Message>(address, loop);
		support = tcp;
		
		tcp.codec(new CodecInitializer() {
			@Override
			public void initPipeline(List<ChannelHandler> p) {
				p.add(new HttpRequestEncoder()); 
				p.add(new HttpResponseDecoder());  
				p.add(new HttpObjectAggregator(loop.getPackageSizeLimit()));
				p.add(new MessageCodec());
			}
		}); 
		
		tcp.startHeartbeat(heartbeatInterval, new HeartbeatMessageBuilder<Message>() { 
			@Override
			public Message build() { 
				Message hbt = new Message();
				hbt.setCommand(Message.HEARTBEAT);
				return hbt;
			} 
		});  
	}
}
 
