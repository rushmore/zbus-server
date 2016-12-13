package io.zbus.net;
 
import java.io.IOException;
import java.util.List;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.zbus.mq.api.Message;
import io.zbus.net.Client.DataHandler;
import io.zbus.net.tcp.TcpClient;
 
public class ClientTest {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		IoDriver ioDriver = new IoDriver();
		Client<Message, Message> client = new TcpClient<Message, Message>("localhost:15555", ioDriver);
		client.codec(new CodecInitializer() { 
			@Override
			public void initPipeline(List<ChannelHandler> p) {
				p.add(new HttpRequestEncoder()); 
				p.add(new HttpResponseDecoder());  
				p.add(new HttpObjectAggregator(32*1024*1024)); 
			}
		});
		
		Message message = new Message();
		message.setCmd("produce");
		message.setTopic("Hong");
		
		
		client.onData(new DataHandler<Message>() { 
			@Override
			public void onData(Message data, Session session) throws IOException {
				System.out.println(data);
			}
		});  
		 
		for(int i=0;i<2;i++){
			client.send(message).sync(); 
		}
		System.out.println("===done===");
		//client.close();
		//ioDriver.close();
	} 
}
