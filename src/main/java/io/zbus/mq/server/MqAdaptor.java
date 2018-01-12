package io.zbus.mq.server;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.ConsumeGroup;
import io.zbus.mq.DiskQueue;
import io.zbus.mq.MemoryQueue;
import io.zbus.mq.Message;
import io.zbus.mq.MessageQueue;
import io.zbus.mq.Protocol;
import io.zbus.mq.Protocol.ConsumeGroupInfo;
import io.zbus.mq.Protocol.ServerEvent;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.mq.disk.DiskMessage;
import io.zbus.mq.server.auth.AuthProvider;
import io.zbus.mq.server.auth.Token;
import io.zbus.rpc.Request;
import io.zbus.transport.MessageHandler;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session; 

public class MqAdaptor extends ServerAdaptor implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(MqAdaptor.class);

	private final Map<String, MessageQueue> mqTable; 
	private final Map<String, MessageHandler<Message>> handlerMap = new ConcurrentHashMap<String, MessageHandler<Message>>();
	private final MqServer mqServer;
	private final MqServerConfig config;    
	private final Tracker tracker; 
	private AuthProvider authProvider;
	private MessageLogger messageLogger;
	
	private ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(16);
	private Set<String> groupOptionalCommands = new HashSet<String>();  
	
	private MonitorAdaptor monitorAdaptor;
 
	public MqAdaptor(MqServer mqServer, MonitorAdaptor monitorAdaptor){
		super(mqServer.getSessionTable());
		
		this.config = mqServer.getConfig();
		this.authProvider = this.config.getAuthProvider();
		
		this.mqServer = mqServer;  
		this.mqTable = mqServer.getMqTable();  
		this.tracker = mqServer.getTracker(); 
		
		this.monitorAdaptor = monitorAdaptor; //if null, no monitor embedded
		 
		groupOptionalCommands.add(Protocol.CONSUME);
		groupOptionalCommands.add(Protocol.DECLARE);
		groupOptionalCommands.add(Protocol.QUERY);
		groupOptionalCommands.add(Protocol.REMOVE); 
		groupOptionalCommands.add(Protocol.EMPTY); 
		
		
		//Produce/Consume
		registerHandler(Protocol.PRODUCE, produceHandler); 
		registerHandler(Protocol.CONSUME, consumeHandler);   
		registerHandler(Protocol.UNCONSUME, unconsumeHandler); 
		registerHandler(Protocol.ROUTE, routeHandler);  
		registerHandler(Protocol.ACK, ackHandler); 
		
		//Topic/ConsumerGroup 
		registerHandler(Protocol.DECLARE, declareHandler);  
		registerHandler(Protocol.QUERY, queryHandler);
		registerHandler(Protocol.REMOVE, removeHandler); 
		registerHandler(Protocol.EMPTY, emptyHandler); 
		
		//Tracker  
		registerHandler(Protocol.TRACK_PUB, trackPubServerHandler); 
		registerHandler(Protocol.TRACK_SUB, trackSubHandler); 
		registerHandler(Protocol.TRACKER, trackerHandler); 
		
		registerHandler(Protocol.SERVER, serverHandler);  
		registerHandler(Protocol.SSL, sslHandler);  
		
		registerHandler(Message.HEARTBEAT, heartbeatHandler);     
	}   
	
	private MessageHandler<Message> produceHandler = new MessageHandler<Message>() { 
		@Override
		public void handle(final Message msg, final Session sess) throws IOException {  
			boolean ok = validateMessage(msg,sess);
			if(!ok) return; 
			
			final MessageQueue mq = findMQ(msg, sess);
			if(mq == null) return; 
			
			int mask = mq.getMask() & (Protocol.MASK_RPC | Protocol.MASK_PROXY);  
			boolean ack = msg.isAck();  
			if(mask != 0){
				ack = false;
			} 
			
			msg.removeHeader(Protocol.COMMAND);
			msg.removeHeader(Protocol.ACK);  
			msg.removeHeader(Protocol.TOKEN);
			mq.produce(msg);  
			
			if(ack){
				ReplyKit.reply200(msg, sess);
			}
		}
	};  
	
	private MessageHandler<Message> consumeHandler = new MessageHandler<Message>() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException {  
			MessageQueue mq = findMQ(msg, sess);
			if(mq == null) return; 
			
			mq.consume(msg, sess);  
			String topic = sess.attr(Protocol.TOPIC);
			if(!msg.getTopic().equalsIgnoreCase(topic)){
				sess.attr(Protocol.TOPIC, mq.topic()); //mark
				
				tracker.myServerChanged(); 
			} 
		}
	}; 
	
	private MessageHandler<Message> unconsumeHandler = new MessageHandler<Message>() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException {  
			MessageQueue mq = findMQ(msg, sess);
			if(mq == null) return; 
			
			mq.unconsume(msg, sess);   
			
			String topic = sess.attr(Protocol.TOPIC);
			if(msg.getTopic().equalsIgnoreCase(topic)){ 
				tracker.myServerChanged(); 
			} 
		}
	}; 
	
	private MessageHandler<Message> ackHandler = new MessageHandler<Message>() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException {   
			MessageQueue mq = findMQ(msg, sess);
			boolean ack = msg.isAck();
			if(mq == null) {
				if(ack) {
					ReplyKit.reply404(msg, sess);
				}
				return;  
			}
			
			mq.ack(msg, sess);
			
			if(ack) {
				ReplyKit.reply200(msg, sess);
			}
		}
	}; 
	
	private MessageHandler<Message> routeHandler = new MessageHandler<Message>() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			String recver = msg.getReceiver();
			if(recver == null) {
				return; //just ignore
			}
			Session target = sessionTable.get(recver);
			if(target == null) {
				log.warn("Missing target %s", recver); 
				return; //just ignore
			} 
			msg.removeHeader(Protocol.ACK);
			msg.removeHeader(Protocol.RECVER);
			msg.removeHeader(Protocol.COMMAND);
			msg.removeHeader("remote-addr");
			
			Integer status = 200;
			if(msg.getOriginStatus() != null){
				status = msg.getOriginStatus(); 
				msg.removeHeader(Protocol.ORIGIN_STATUS);
			} 
			msg.setStatus(status);
			
			try{
				target.write(msg);
			} catch(Exception ex){
				log.warn("Target(%s) write failed, Ignore", recver); 
				return; 
			}
		}
	};  
	
	private MessageHandler<Message> declareHandler = new MessageHandler<Message>() {  
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			String topic = msg.getTopic();    
			
			if(StrKit.isEmpty(topic)){ 
				ReplyKit.reply400(msg, sess, "Missing topic");
				return;
			}
			topic = topic.trim();  
			Integer topicMask = msg.getTopicMask();  
    		MessageQueue mq = null;
    		synchronized (mqTable) {
    			mq = mqTable.get(topic);  
    			if(mq == null){ 
    				if(topicMask != null && (topicMask&Protocol.MASK_MEMORY) != 0){
    					mq = new MemoryQueue(topic);
    				} else {
    					mq = new DiskQueue(new File(config.getMqPath(), topic));  
    				} 
	    			mq.setCreator(msg.getToken()); 
	    			mq.setMessageLogger(messageLogger);
	    			mqTable.put(topic, mq);
	    			log.info("MQ Created: %s", mq);
    			}
    		} 
			
			try { 
				if(topicMask != null){
					mq.setMask(topicMask);
				}
				
				String groupName = msg.getConsumeGroup();  
				ConsumeGroup consumeGroup = new ConsumeGroup(msg); 
				if(consumeGroup.getGroupNameAuto() != null && consumeGroup.getGroupNameAuto()) {
					//Internally generated ConsumeGroup enabled
					ConsumeGroupInfo info = mq.declareGroup(consumeGroup);  
					ReplyKit.replyJson(msg, sess, info); 
				} else { 
					if(groupName != null){  //ConsumeGroup specified 
						ConsumeGroupInfo info = mq.declareGroup(consumeGroup); 
						ReplyKit.replyJson(msg, sess, info); 
					} else {
						TopicInfo topicInfo = mq.topicInfo();
				    	topicInfo.serverAddress = mqServer.getServerAddress();  
						ReplyKit.replyJson(msg, sess, topicInfo);
					}
				}
				
			} catch (Exception e) { 
				log.error(e.getMessage(), e);
				ReplyKit.reply500(msg, sess, e); 
			} 
			
			tracker.myServerChanged();  
		}
	};
	
	private MessageHandler<Message> queryHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException { 
			Token token = authProvider.getToken(msg.getToken());
			if(msg.getTopic() == null){   
				ServerInfo info = mqServer.serverInfo();
				info = Token.filter(info, token);
				if(info == null){
					ReplyKit.reply404(msg, sess);
				} else {
					ReplyKit.replyJson(msg, sess, tracker.serverInfo(token));
				}
				
				return;
			} 
			
			MessageQueue mq = findMQ(msg, sess);
	    	if(mq == null){ 
	    		ReplyKit.reply404(msg, sess);
				return;
			}
	    	TopicInfo topicInfo = mq.topicInfo();
	    	topicInfo.serverAddress = mqServer.getServerAddress(); 
	    	
			String group = msg.getConsumeGroup();
			if(group == null){
				topicInfo = Token.filter(topicInfo, token);
				if(topicInfo == null){
					ReplyKit.reply404(msg, sess);
				} else {
					ReplyKit.replyJson(msg, sess, topicInfo);
				}
				return;
			}
	    	
			ConsumeGroupInfo groupInfo = topicInfo.consumeGroup(group); 
	    	if(groupInfo == null){
	    		String hint = String.format("404: ConsumeGroup(%s) Not Found", group);
	    		ReplyKit.reply404(msg, sess, hint);
	    		return;
	    	}
	    	ReplyKit.replyJson(msg, sess, groupInfo);  
		}  
	}; 
	
	private MessageHandler<Message> emptyHandler = new MessageHandler<Message>() { 
		@Override
		public void handle(Message msg, Session session) throws IOException { 
			ReplyKit.reply200(msg, session);
		}   
	};
	
	private MessageHandler<Message> removeHandler = new MessageHandler<Message>() {  
		@Override
		public void handle(Message msg, Session sess) throws IOException {  
			String topic = msg.getTopic(); 
			if(StrKit.isEmpty(topic)){ 
				ReplyKit.reply400(msg, sess, "Missing topic");
				return;
			} 
			topic = topic.trim();   
			MessageQueue mq = mqTable.get(topic);
			if(mq == null){ 
				ReplyKit.reply404(msg, sess);
				return;
			}   
			
			String groupName = msg.getConsumeGroup();
			if(groupName != null){
				try {
					mq.removeGroup(groupName);
					tracker.myServerChanged(); 
					ReplyKit.reply200(msg, sess); 
				} catch (FileNotFoundException e){
					ReplyKit.reply404(msg, sess, "ConsumeGroup("+groupName + ") Not Found"); 
				}  
				return;
			}  
			
			mq = mqTable.remove(mq.topic());
			if(mq != null){
				mq.destroy();
				tracker.myServerChanged(); 
				ReplyKit.reply200(msg, sess);
			} else {
				ReplyKit.reply404(msg, sess, "Topic(" + msg.getTopic() + ") Not Found");
			}
		} 
	}; 
	  
	private MessageHandler<Message> sslHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException { 
			String server = msg.getHeader("server");
			if(StrKit.isEmpty(server)){
				server = mqServer.getServerAddress().address;
			}
			
			String certContent = mqServer.sslCertTable.get(server);
			if(certContent == null){
				ReplyKit.reply404(msg, sess, "Certificate("+server+") Not Found");
				return;
			}
			
			Message res = new Message();
			res.setId(msg.getId());
			res.setStatus(200); 
			res.setBody(certContent);
			sess.write(res); 
		}
	};
	
	private MessageHandler<Message> heartbeatHandler = new MessageHandler<Message>() {
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			// just ignore
		}
	}; 
	 
	private MessageHandler<Message> trackPubServerHandler = new MessageHandler<Message>() {
		
		@Override
		public void handle(Message msg, Session session) throws IOException {  
			try{
				boolean ack = msg.isAck(); 
				ServerEvent event = JsonKit.parseObject(msg.getBodyString(), ServerEvent.class);   
				if(event == null){
					ReplyKit.reply400(msg, session, Protocol.TRACK_PUB + " json required");
					return;
				}
				tracker.serverInTrackUpdated(event); 
				
				if(ack){
					ReplyKit.reply200(msg, session);
				}
			} catch (Exception e) { 
				ReplyKit.reply500(msg, session, e);
			} 
		}
	};
	 
	private MessageHandler<Message> trackSubHandler = new MessageHandler<Message>() {
		
		@Override
		public void handle(Message msg, Session session) throws IOException { 
			tracker.clientSubcribe(msg, session);
		}
	}; 
	
	private MessageHandler<Message> trackerHandler = new MessageHandler<Message>() { 
		@Override
		public void handle(Message msg, Session session) throws IOException { 
			Token token = authProvider.getToken(msg.getToken()); 
			ReplyKit.replyJson(msg, session, tracker.trackerInfo(token)); 
		}
	}; 
	
	private MessageHandler<Message> serverHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {  
			Token token = authProvider.getToken(msg.getToken()); 
			ReplyKit.replyJson(msg, sess, tracker.serverInfo(token)); 
		}
	}; 
	
	protected void cleanSession(Session sess) throws IOException{
		super.cleanSession(sess);
		
		String topic = sess.attr(Protocol.TOPIC);
		if(topic != null){
			MessageQueue mq = mqTable.get(topic); 
			if(mq != null){
				mq.cleanSession(sess); 
				tracker.myServerChanged();
			}
		}
		
		tracker.cleanSession(sess);  
	}  
    
	public void loadDiskQueue() throws IOException {
		log.info("Loading DiskQueues...");
		mqTable.clear();
		
		File[] mqDirs = new File(config.getMqPath()).listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		
		if (mqDirs != null && mqDirs.length > 0) {
			for (File mqDir : mqDirs) {
				MessageQueue mq = new DiskQueue(mqDir);
				mq.setMessageLogger(messageLogger);
				mqTable.put(mqDir.getName(), mq);
				log.info("Topic = %s loaded", mqDir.getName()); 
			}
		}   
	}
    
    public void close() throws IOException {     
    	if(this.timer != null){
    		this.timer.shutdown();
    	}
    }  
    
    private void handleUrlMessage(Message msg){ 
    	if(msg.getCommand() != null){ //if cmd in header, URL parsing is ignored!
    		return;
    	} 
    	String url = msg.getUrl(); 
    	if(url == null || "/".equals(url)){
    		msg.setCommand(Protocol.HOME);
    		return;
    	} 
    	UrlInfo info = HttpKit.parseUrl(url); 
    	msg.merge(info.params);
    	String cmd = info.params.get(Protocol.COMMAND); 
    	if(cmd == null) {
    		if(info.path.isEmpty()) {
    			cmd = Protocol.HOME;
    		} else {
    			cmd = Protocol.PRODUCE;
    		}
    	} 
    	
    	cmd = cmd.toLowerCase();
    	msg.setCommand(cmd); 
    	if(msg.getTopic() == null) {
    		if(info.path.size() > 0){
    			msg.setTopic(info.path.get(0));
    		}
    	}
    	if(groupOptionalCommands.contains(cmd)){
    		if(msg.getConsumeGroup() == null){
    			if(info.path.size() > 1){
    				msg.setConsumeGroup(info.path.get(1));
    			}
    		}
    	} 
    	
    	//special handle for token
    	if(msg.getToken() == null){
    		String token = info.params.get(Protocol.TOKEN); 
    		if(token != null){
    			msg.setToken(token);
    		}
    	} 
    	
    	if(msg.getBody() != null) return;
    	 
    	MessageQueue mq = null;
    	String topic = msg.getTopic();
    	if(topic != null){
    		mq = mqTable.get(topic);
    	}
    	if(mq == null) return;
    	
    	if((mq.getMask()&Protocol.MASK_RPC) == 0) return;  //Not RPC
    	
    	// /topic/module/<topic>/<method>/<param_1>/../<param_n>   
    	Request req = new Request();
    	if(info.path.size()>=2){
    		req.setModule(info.path.get(1));
    	}
    	if(info.path.size()>=3){
    		req.setMethod(info.path.get(2));
    	}
    	if(info.path.size()>3){
    		Object[] params = new Object[info.path.size()-3];
    		for(int i=0;i<params.length;i++){
    			params[i] = info.path.get(3+i);
    		}
    		req.setParams(params); 
    	}  
    	msg.setAck(false);
    	msg.setJsonBody(JsonKit.toJSONString(req));   
	} 
    
    public void onMessage(Object obj, Session sess) throws IOException {  
    	Message msg = (Message)obj;  
    	msg.setSender(sess.id());
		msg.setHost(mqServer.getServerAddress().address); 
		msg.setRemoteAddr(sess.remoteAddress());
		if(msg.getId() == null){
			msg.setId(UUID.randomUUID().toString());
		}
		msg.parseCookieToken();
		
		if(messageLogger != null){
			messageLogger.log(msg, sess);
		} 
		
		handleUrlMessage(msg);   
		
		if(monitorAdaptor != null && monitorAdaptor.handle(msg, sess)){//try monitor command first
			return;
		}
		
		String cmd = msg.getCommand();  
		boolean auth = true;
		if(!Protocol.HEARTBEAT.equals(cmd)){
			auth = authProvider.auth(msg); 
		}
		if(!auth){
			ReplyKit.reply403(msg, sess);
			return;
		}
		
    	if(cmd != null){
    		MessageHandler<Message> handler = handlerMap.get(cmd);
	    	if(handler != null){
	    		handler.handle(msg, sess);
	    		return;
	    	}
    	}
    	
    	String topic = cmd; //treat cmd as topic to support proxy URL
    	MessageQueue mq = null;
    	if(topic != null){
    		mq = mqTable.get(topic); 
    	}
    	if(mq == null || cmd == null){
    		Message res = new Message();
        	res.setId(msg.getId()); 
        	res.setStatus(400);
        	String text = String.format("Bad format: command(%s) not support", cmd);
        	if(Protocol.HOME.equals(cmd)){
        		text = "<h3>Welcome to zbus, contact admin to get monitor page!</h3>";
        	}
        	res.setBody(text); 
        	res.setHeader("content-type", "text/html");
        	sess.write(res);
        	return;
    	} 
    }  
	
    private MessageQueue findMQ(Message msg, Session sess) throws IOException{
		String topic = msg.getTopic();
		boolean isAck = msg.isAck();
		if(topic == null && isAck){
			ReplyKit.reply400(msg, sess, "Missing topic"); 
    		return null;
		}
		
		MessageQueue mq = mqTable.get(topic); 
    	if(mq == null && isAck){
    		ReplyKit.reply404(msg, sess); 
    		return null;
    	}   
    	return mq;
	}

	private boolean validateMessage(Message msg, Session session) throws IOException{
		final boolean ack = msg.isAck();  
		String id = msg.getId();
		String tag = msg.getTag();
		if(id != null && id.length()>DiskMessage.ID_MAX_LEN){ 
			if(ack) ReplyKit.reply400(msg, session, "Message.Id length should <= "+DiskMessage.ID_MAX_LEN);
			return false;
		}
		if(tag != null && tag.length()>DiskMessage.TAG_MAX_LEN){ 
			if(ack) ReplyKit.reply400(msg, session, "Message.Tag length should <= "+DiskMessage.TAG_MAX_LEN);
			return false;
		}
		return true;
	}
    
    public void registerHandler(String command, MessageHandler<Message> handler){
    	this.handlerMap.put(command, handler);
    }

	public void setMessageLogger(MessageLogger messageLogger) {
		this.messageLogger = messageLogger;
	}  
}