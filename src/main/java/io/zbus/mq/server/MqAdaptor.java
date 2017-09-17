package io.zbus.mq.server;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.zbus.kit.FileKit;
import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.kit.StrKit.UrlInfo;
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
	private boolean verbose = false;    
	private final MqServer mqServer;
	private final MqServerConfig config;    
	private final Tracker tracker; 
	private AuthProvider authProvider;
	
	private ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(16);
	private Set<String> groupOptionalCommands = new HashSet<String>(); 
	private Set<String> exemptAuthCommands = new HashSet<String>(); 
 
	public MqAdaptor(MqServer mqServer){
		super(mqServer.getSessionTable());
		
		this.config = mqServer.getConfig();
		this.authProvider = this.config.getAuthProvider();
		
		this.mqServer = mqServer;  
		this.mqTable = mqServer.getMqTable();  
		this.tracker = mqServer.getTracker(); 
		 
		groupOptionalCommands.add(Protocol.CONSUME);
		groupOptionalCommands.add(Protocol.DECLARE);
		groupOptionalCommands.add(Protocol.QUERY);
		groupOptionalCommands.add(Protocol.REMOVE); 
		groupOptionalCommands.add(Protocol.EMPTY);
		
		exemptAuthCommands.add(Protocol.HEARTBEAT);
		exemptAuthCommands.add(Protocol.JS);
		exemptAuthCommands.add(Protocol.CSS);
		exemptAuthCommands.add(Protocol.IMG);
		exemptAuthCommands.add(Protocol.LOGIN);
		exemptAuthCommands.add(Protocol.LOGOUT);
		exemptAuthCommands.add(Protocol.PAGE); 
		exemptAuthCommands.add("favicon.ico"); 
		
		
		//Produce/Consume
		registerHandler(Protocol.PRODUCE, produceHandler); 
		registerHandler(Protocol.CONSUME, consumeHandler);  
		registerHandler(Protocol.ROUTE, routeHandler);  
		registerHandler(Protocol.UNCONSUME, unconsumeHandler); 
		
		//Topic/ConsumerGroup 
		registerHandler(Protocol.DECLARE, declareHandler);  
		registerHandler(Protocol.QUERY, queryHandler);
		registerHandler(Protocol.REMOVE, removeHandler); 
		
		//Tracker  
		registerHandler(Protocol.TRACK_PUB, trackPubServerHandler); 
		registerHandler(Protocol.TRACK_SUB, trackSubHandler); 
		registerHandler(Protocol.TRACKER, trackerHandler); 
		
		registerHandler(Protocol.SERVER, serverHandler); 
		
		registerHandler(Protocol.SSL, sslHandler); 
		
		
		//Monitor/Management
		registerHandler(Protocol.HOME, homeHandler);  
		registerHandler("favicon.ico", faviconHandler);
		
		registerHandler(Protocol.LOGIN, loginHandler);  
		registerHandler(Protocol.LOGOUT, logoutHandler);  
		registerHandler(Protocol.JS, jsHandler); 
		registerHandler(Protocol.CSS, cssHandler);
		registerHandler(Protocol.IMG, imgHandler); 
		registerHandler(Protocol.PAGE, pageHandler);
		registerHandler(Protocol.PING, pingHandler);   
		
		registerHandler(Message.HEARTBEAT, heartbeatHandler);    
		
		
		if(Fix.Enabled){
			//Compatible to older zbus
			registerHandler(Fix.CreateMQ, declareHandler); 
			registerHandler(Fix.QueryMQ, queryHandler); 
			registerHandler(Fix.RemoveMQ, removeHandler); 
		} 
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
	    			mqTable.put(topic, mq);
	    			log.info("MQ Created: %s", mq);
    			}
    		} 
			
			try { 
				if(topicMask != null){
					mq.setMask(topicMask);
				}
				
				String groupName = msg.getConsumeGroup();  
				if(groupName != null){   
					ConsumeGroup consumeGroup = new ConsumeGroup(msg);  
					ConsumeGroupInfo info = mq.declareGroup(consumeGroup); 
					ReplyKit.replyJson(msg, sess, info); 
				} else { 
					if(mq.groupInfo(topic) == null){
						ConsumeGroup consumeGroup = new ConsumeGroup(msg);  
						mq.declareGroup(consumeGroup); 
					}
					TopicInfo topicInfo = mq.topicInfo();
			    	topicInfo.serverAddress = mqServer.getServerAddress();  
					ReplyKit.replyJson(msg, sess, topicInfo);
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
	
	private MessageHandler<Message> pingHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = new Message();
			res.setStatus(200); 
			res.setId(msg.getId()); 
			res.setBody(""+System.currentTimeMillis());
			sess.write(res);
		}
	};
	 	 
	
	private MessageHandler<Message> homeHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {  
			String tokenStr = msg.getToken();
			Token token = authProvider.getToken(tokenStr);
			Map<String, Object> model = new HashMap<String, Object>();
			String tokenShow = null;
			if(token != null && tokenStr != null){
				tokenShow = String.format("<li><a href='/?cmd=logout'>%s Logout</a></li>", token.name);
			}
			model.put("token", tokenShow);
			
			ReplyKit.replyTemplate(msg, sess, "home.htm", model);
		}
	};  
	
	private MessageHandler<Message> loginHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {  
			if("GET".equals(msg.getMethod())){
				ReplyKit.replyTemplate(msg, sess, "login.htm"); 
				return;
			} 
			
			Map<String, String> data = StrKit.kvp(msg.getBodyString(), "&"); 
			String tokenstr = null;
			if(data.containsKey(Protocol.TOKEN)) {
				tokenstr = data.get(Protocol.TOKEN);
			}
			Token token = authProvider.getToken(tokenstr); 
			
			Message res = new Message(); 
			if(token == null){
				res.setHeader("location", "/?cmd=login"); 
				res.setStatus(302); 
				sess.write(res);
				return;
			} 
			
			if(token != null){
				Cookie cookie = new DefaultCookie(Protocol.TOKEN, tokenstr); 
				res.setHeader("Set-Cookie", ServerCookieEncoder.STRICT.encode(cookie));
			} 
			res.setHeader("location", "/"); 
			res.setStatus(302); //redirect to home page
			sess.write(res);
		}
	};  
	
	private MessageHandler<Message> logoutHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {  
			Message res = new Message();  
			res.setId(msg.getId());
			res.setHeader("location", "/?cmd=login"); 
			
			Cookie cookie = new DefaultCookie(Protocol.TOKEN, "");
			cookie.setMaxAge(0);
			res.setHeader("Set-Cookie", ServerCookieEncoder.STRICT.encode(cookie)); 
			res.setStatus(302); 
			sess.write(res); 
		}
	};  
	
	private Message handleTemplateRequest(Message msg){
		return handleTemplateRequest(msg, null);
	}
	
	private Message handleTemplateRequest(Message msg, Map<String, Object> model){
		Message res = new Message();  
		String fileName = msg.getTopic();
		String cmd = msg.getCommand();  
		String body = null;
		try{
			body = FileKit.renderFile(fileName, model);
			if(body == null){
				res.setStatus(404);
				body = "404: File (" + fileName +") Not Found";
			} else {
				res.setStatus(200); 
			}
		} catch (IOException e){
			res.setStatus(404);
			body = e.getMessage();
		}  
		res.setBody(body); 
		if(Protocol.JS.equals(cmd)){
			res.setHeader("content-type", "application/javascript");
		} else if(Protocol.CSS.equals(cmd)){
			res.setHeader("content-type", "text/css");
		} else if(Protocol.IMG.equals(cmd)){
			if("favicon.ico".equals(fileName)){
				res.setHeader("content-type", "image/x-icon");
			} else {
				res.setHeader("content-type", "image/svg+xml");
			}
		} else {
			res.setHeader("content-type", "text/html");
		}
		return res;
	}
	
	 
	
	private MessageHandler<Message> pageHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {  
			Message res = handleTemplateRequest(msg); 
			sess.write(res); 
		}
	};
	
	private MessageHandler<Message> jsHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = handleTemplateRequest(msg); 
			sess.write(res); 
		}
	};
	
	private MessageHandler<Message> cssHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = handleTemplateRequest(msg); 
			sess.write(res); 
		}
	}; 
	
	private MessageHandler<Message> imgHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = handleTemplateRequest(msg); 
			sess.write(res); 
		}
	}; 
	
	private MessageHandler<Message> faviconHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = handleTemplateRequest(msg); 
			sess.write(res); 
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
	
    public void setVerbose(boolean verbose) {
		this.verbose = verbose;
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
    	UrlInfo info = StrKit.parseUrl(url); 
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
    	
    	boolean rpc = false; 
    	MessageQueue mq = null;
    	String topic = msg.getTopic();
    	if(topic != null){
    		mq = mqTable.get(topic);
    	}
    	if(mq != null){
    		rpc = (mq.getMask()&Protocol.MASK_RPC) != 0;
    	}
    	if(rpc){
    		// /<topic>/<method>/<param_1>/../<param_n>[?module=<module>&&<header_ext_kvs>]  
    		String method = "";
    		if(info.path.size()>=2){
    			method = info.path.get(1);
    		}
    		
    		Request req = new Request();
        	req.setMethod(method); 
        	if(info.params.containsKey("module")){
        		req.setModule(info.params.get("module"));
        	} 
        	if(info.path.size()>2){
        		Object[] params = new Object[info.path.size()-2];
        		for(int i=0;i<params.length;i++){
        			params[i] = info.path.get(2+i);
        		}
        		req.setParams(params); 
        	}  
        	msg.setAck(false);
        	msg.setBody(JsonKit.toJSONString(req));
    	} 
	}
    
    private void parseCookieToken(Message msg){
    	String cookieString = msg.getHeader("cookie");
        if (cookieString != null) {
        	Map<String, String> cookies = StrKit.kvp(cookieString, "[;]");
        	if(cookies.containsKey(Protocol.TOKEN)){
        		if(msg.getToken() == null){
        			msg.setToken(cookies.get(Protocol.TOKEN));
        		} 
        	} 
        }
    }
     
    
    public void onMessage(Object obj, Session sess) throws IOException {  
    	Message msg = (Message)obj;  
    	msg.setSender(sess.id());
		msg.setHost(mqServer.getServerAddress().address); 
		msg.setRemoteAddr(sess.remoteAddress());
		if(msg.getId() == null){
			msg.setId(UUID.randomUUID().toString());
		}
		parseCookieToken(msg);
		
		if(verbose){
			log.info("\n%s", msg);
		} 
		
		
		handleUrlMessage(msg); 
		
		String cmd = msg.getCommand();
		boolean auth = true;
		if(!exemptAuthCommands.contains(cmd)){
			auth = authProvider.auth(msg);
		}
		
		if(!auth){ 
			if(Protocol.HOME.equals(cmd)){
				ReplyKit.reply302(msg, sess, "/?cmd=login");
			} else { 
				ReplyKit.reply403(msg, sess);
			} 
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
        	res.setBody(text); 
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
}