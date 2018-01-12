package io.zbus.mq;

import java.io.IOException;
import java.util.List;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.zbus.kit.JsonKit;
import io.zbus.mq.Protocol.ConsumeGroupInfo;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.mq.Protocol.TrackerInfo;
import io.zbus.mq.server.MqServer;
import io.zbus.transport.CodecInitializer;
import io.zbus.transport.CompositeClient;
import io.zbus.transport.EventLoop;
import io.zbus.transport.ResultCallback;
import io.zbus.transport.ServerAddress;
import io.zbus.transport.inproc.InProcClient;
import io.zbus.transport.tcp.TcpClient;
import io.zbus.transport.tcp.TcpClient.HeartbeatMessageBuilder;
 

public class MqClient extends CompositeClient<Message, Message> {         
	protected String token;      
	protected long invokeTimeout = 3000;
	protected long heartbeatInterval = 60000; //60s 
	
	public MqClient(String address, final EventLoop loop){
		ServerAddress serverAddress = new ServerAddress(address);
		buildSupport(serverAddress, loop, heartbeatInterval);
	}   
	
	public MqClient(String address, final EventLoop loop, int heartbeatInterval){
		ServerAddress serverAddress = new ServerAddress(address);
		buildSupport(serverAddress, loop, heartbeatInterval);
	}
	
	/**
	 * In-Process MqClient, optimized for speed.
	 * 
	 * @param mqServer MqServer instance in the process, no need to start 
	 */
	public MqClient(MqServer mqServer) {
		ServerAddress serverAddress = new ServerAddress();
		serverAddress.setServer(mqServer);
		
		buildSupport(serverAddress, null, heartbeatInterval);
	} 
	
	public MqClient(ServerAddress serverAddress, final EventLoop loop){  
		buildSupport(serverAddress, loop, heartbeatInterval);
	}
	
	public MqClient(ServerAddress serverAddress, final EventLoop loop, long heartbeatInterval){  
		buildSupport(serverAddress, loop, heartbeatInterval);
	}
	
	private void buildSupport(ServerAddress serverAddress, final EventLoop loop, long heartbeatInterval){
		this.token = serverAddress.getToken();
		if(serverAddress.server != null){
			support = new InProcClient<Message, Message>(serverAddress.server);
			return;
		} 
		String address = serverAddress.address;
		if(address == null){
			throw new IllegalArgumentException("ServerAddress missing address property");
		}
		
		if (address.startsWith("ipc://")) {
			throw new IllegalArgumentException("IPC not implemented yet!");
			//TODO IPC client support
		}
		
		//default to TCP 
		if(address.startsWith("tcp://")){
			serverAddress.address = address.substring("tcp://".length());
		}
			
		TcpClient<Message, Message> tcp = new TcpClient<Message, Message>(serverAddress, loop);
		support = tcp;
		tcp.codec(new CodecInitializer() {
			@Override
			public void initPipeline(List<ChannelHandler> p) {
				p.add(new HttpRequestEncoder()); 
				p.add(new HttpResponseDecoder());  
				p.add(new HttpObjectAggregator(loop.getPackageSizeLimit()));
				p.add(new io.zbus.transport.http.MessageCodec());
				p.add(new io.zbus.mq.MessageCodec());
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
	
	
	public void setInvokeTimeout(long invokeTimeout) {
		this.invokeTimeout = invokeTimeout;
	}
	
	public Message produce(Message msg, long timeout) throws IOException, InterruptedException{ 
		msg.setCommand(Protocol.PRODUCE);
		return invokeSync(msg, timeout);  
	} 
	
	public Message produce(Message msg) throws IOException, InterruptedException{ 
		return produce(msg, invokeTimeout);  
	} 
	
	public void produceAsync(Message msg, ResultCallback<Message> callback) throws IOException {
		msg.setCommand(Protocol.PRODUCE);
		invokeAsync(msg, callback);
	}
	
	public Message consume(String topic) throws IOException, InterruptedException{
		ConsumeCtrl ctrl = new ConsumeCtrl();
		ctrl.setTopic(topic);
		ctrl.setConsumeGroup(topic); 
		return consume(ctrl);
	}
	
	public Message consume(String topic, String group) throws IOException, InterruptedException { 
		ConsumeCtrl ctrl = new ConsumeCtrl();
		ctrl.setTopic(topic);
		ctrl.setConsumeGroup(group); 
		return consume(ctrl);
	} 
	
	public Message consume(ConsumeCtrl ctrl) throws IOException, InterruptedException {
		if(ctrl.getTopic() == null) {
			throw new IllegalArgumentException("Missing topic");
		}
		
		Message msg = new Message();
		msg.setCommand(Protocol.CONSUME);
		msg.setTopic(ctrl.getTopic());
		msg.setConsumeGroup(ctrl.getConsumeGroup());
		msg.setOffset(ctrl.getOffset());
		msg.setConsumeWindow(ctrl.getConsumeWindow()); 
		
		Message res = invokeSync(msg, invokeTimeout);
		if (res == null) return res;
		
		res.setId(res.getOriginId());
		res.removeHeader(Protocol.ORIGIN_ID); 
		return res;
	}
	
	public void unconsume(String topic) throws IOException, InterruptedException {
		unconsume(topic, topic);
	}
	
	public void unconsume(String topic, String group) throws IOException, InterruptedException {
		Message msg = new Message();
		msg.setCommand(Protocol.UNCONSUME);
		msg.setTopic(topic);
		msg.setConsumeGroup(group);   
		invokeAsync(msg, null); 
	}
	
	public void ack(Message res) throws IOException {
		Message msg = new Message();
		msg.setCommand(Protocol.ACK);
		msg.setTopic(res.getTopic());
		msg.setConsumeGroup(res.getConsumeGroup());     
		msg.setOffset(res.getOffset());
		
		try {
			invokeSync(msg);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} 
	}
	
	public void ackAsync(Message res) throws IOException {
		Message msg = new Message();
		msg.setCommand(Protocol.ACK);
		msg.setTopic(res.getTopic());
		msg.setConsumeGroup(res.getConsumeGroup());   
		msg.setOffset(res.getOffset());
		
		msg.setAck(false); //No need to reply from server, Not ACK command.
		invokeAsync(msg, null); 
	}
	 
	
	public TrackerInfo queryTracker() throws IOException, InterruptedException{
		Message msg = new Message();
		msg.setCommand(Protocol.TRACKER); 
		  
		Message res = invokeSync(msg, invokeTimeout);
		return parseResult(res, TrackerInfo.class); 
	}
	
	public ServerInfo queryServer() throws IOException, InterruptedException{
		Message msg = new Message();
		msg.setCommand(Protocol.SERVER); 
		  
		Message res = invokeSync(msg, invokeTimeout);
		return parseResult(res, ServerInfo.class); 
	}
	
	
	public String querySslCertificate(String server) throws IOException, InterruptedException{
		Message msg = new Message();
		msg.setCommand(Protocol.SSL); 
		msg.setHeader("server", server);
		  
		Message res = invokeSync(msg, invokeTimeout); 
		if(res.getStatus() != 200){
			return null;
		}
		return res.getBodyString();
	}
	
	 
	public TopicInfo queryTopic(String topic) throws IOException, InterruptedException {
		Message msg = new Message();
		msg.setCommand(Protocol.QUERY);
		msg.setTopic(topic); 
		 
		Message res = invokeSync(msg, invokeTimeout);
		return parseResult(res, TopicInfo.class); 
	} 
	
	public ConsumeGroupInfo queryGroup(String topic, String group) throws IOException, InterruptedException{
		Message msg = new Message();
		msg.setCommand(Protocol.QUERY);
		msg.setTopic(topic); 
		msg.setConsumeGroup(group);
		  
		Message res = invokeSync(msg, invokeTimeout);
		return parseResult(res, ConsumeGroupInfo.class); 
	}
	
	
	public TopicInfo declareTopic(String topic) throws IOException, InterruptedException{
		Topic ctrl = new Topic();
		ctrl.setName(topic);
		return declareTopic(ctrl);
	} 
	
	public TopicInfo declareTopic(Topic topic) throws IOException, InterruptedException{
		Message msg = new Message();
		msg.setCommand(Protocol.DECLARE);
		topic.writeToMessage(msg);
		 
		Message res = invokeSync(msg, invokeTimeout);
		return parseResult(res, TopicInfo.class); 
	}
	
	public ConsumeGroupInfo declareGroup(String topic, ConsumeGroup group) throws IOException, InterruptedException{
		Topic t = new Topic();
		t.setName(topic);
		return declareGroup(t, group);
	}
	 
	public ConsumeGroupInfo declareGroup(Topic topic, ConsumeGroup group) throws IOException, InterruptedException{
		Message msg = new Message();
		msg.setCommand(Protocol.DECLARE);
		topic.writeToMessage(msg);
		group.writeToMessage(msg); 
		
		Message res = invokeSync(msg, invokeTimeout);
		return parseResult(res, ConsumeGroupInfo.class); 
	}
	
	
	public void removeTopic(String topic) throws IOException, InterruptedException{
		removeGroup(topic, null);
	}
	
	public void removeGroup(String topic, String group) throws IOException, InterruptedException{
		Message msg = new Message();
		msg.setCommand(Protocol.REMOVE);
		msg.setTopic(topic); 
		msg.setConsumeGroup(group);
		 
		Message res = invokeSync(msg, invokeTimeout);
		checkResult(res);
	}   
	 
	
	public void emptyTopic(String topic) throws IOException, InterruptedException{
		emptyGroup(topic, null);
	}    
	
	public void emptyGroup(String topic, String group) throws IOException, InterruptedException{
		Message msg = new Message();
		msg.setCommand(Protocol.EMPTY);
		msg.setTopic(topic); 
		msg.setConsumeGroup(group);
		 
		Message res = invokeSync(msg, invokeTimeout);
		checkResult(res); 
	}
	
	public void route(Message msg) throws IOException{
		msg.setCommand(Protocol.ROUTE);  
		msg.setAck(false); 
		//invoke message should be request typed, if not add origin_status header and change it to request type
		Integer status = msg.getStatus();
		if(status != null){
			msg.setOriginStatus(status); 
			msg.setStatus(null); //make it as request 
		} 
		
		invokeAsync(msg, null);  
	}  
	
	public Message invokeSync(Message msg, long timeout) throws IOException, InterruptedException {
		fillCommonHeaders(msg);
		return super.invokeSync(msg, timeout);
	}
	
	public Message invokeSync(Message msg) throws IOException, InterruptedException {
		return invokeSync(msg, invokeTimeout);
	}
	
	public void invokeAsync(Message msg, ResultCallback<Message> callback) throws IOException {
		fillCommonHeaders(msg);
	    super.invokeAsync(msg, callback);
	}
	
	
	
	
	private void fillCommonHeaders(Message msg){  
		if(msg.getToken() == null){
			msg.setToken(this.token);
		}
		msg.setVersion(Protocol.VERSION_VALUE); //Set Version
	}
	
	private void checkResult(Message msg){
		if(msg.getStatus() != 200 ){
			throw new MqException(msg.getBodyString());
		}
	}
	
	private <T> T parseResult(Message msg, Class<T> clazz){
		checkResult(msg); 
		
		try{
			return JsonKit.parseObject(msg.getBodyString(), clazz);
		} catch (Exception e) {
			throw new MqException(msg.getBodyString(), e);
		}
	} 

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}  
}
