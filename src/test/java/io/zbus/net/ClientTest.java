package io.zbus.net;
 
import java.io.IOException;
import java.util.List;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.zbus.net.SimpleClient.DataHandler;
import io.zbus.net.Sync.Id;
import io.zbus.net.http.Message;
import io.zbus.net.http.MessageToHttpWsCodec;
import io.zbus.net.tcp.TcpClient;

class Msg implements Id{
	private String id;
	private String body;
	@Override
	public String getId() {
		return id;
	}
	
	@Override
	public void setId(String id) { 
		this.id = id;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
	
	
}

public class ClientTest {

	public static void main(String[] args) throws Exception {
		IoDriver ioDriver = new IoDriver();
		DefaultClient<Message, Message> client = new DefaultClient<Message, Message>("localhost:15555", ioDriver);
		client.codec(new CodecInitializer() { 
			@Override
			public void initPipeline(List<ChannelHandler> p) {
				p.add(new HttpRequestEncoder()); 
				p.add(new HttpResponseDecoder());  
				p.add(new HttpObjectAggregator(32*1024*1024));
				p.add(new MessageToHttpWsCodec());
			}
		});
		
		Message message = new Message();
		message.setCmd("produce");
		message.setMq("hong");
		
		client.onData(new DataHandler<Message>() { 
			@Override
			public void onData(Message data, Session session) throws IOException {
				System.out.println(data);
			}
		});
		client.connect().sync();
		client.send(message);
	} 
}
