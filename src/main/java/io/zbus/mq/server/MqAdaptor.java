package io.zbus.mq.server;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import io.zbus.kit.FileKit;
import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.ConsumeGroup;
import io.zbus.mq.DiskQueue;
import io.zbus.mq.Message;
import io.zbus.mq.MessageQueue;
import io.zbus.mq.Protocol;
import io.zbus.mq.Protocol.ConsumeGroupInfo;
import io.zbus.mq.Protocol.ServerEvent;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.mq.disk.DiskMessage;
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
	
	private ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(16);
	private Set<String> restUrlCommands = new HashSet<String>(); 
 
	public MqAdaptor(MqServer mqServer){
		super(mqServer.getSessionTable());
		
		this.config = mqServer.getConfig();
		
		this.mqServer = mqServer; 
		this.mqTable = mqServer.getMqTable();  
		this.tracker = mqServer.getTracker(); 
		
		restUrlCommands.add(Protocol.PRODUCE);
		restUrlCommands.add(Protocol.CONSUME);
		restUrlCommands.add(Protocol.DECLARE);
		restUrlCommands.add(Protocol.QUERY);
		restUrlCommands.add(Protocol.REMOVE); 
		restUrlCommands.add(Protocol.EMPTY);
		
		
		//Produce/Consume
		registerHandler(Protocol.PRODUCE, produceHandler); 
		registerHandler(Protocol.CONSUME, consumeHandler);  
		registerHandler(Protocol.ROUTE, routeHandler); 
		registerHandler(Protocol.RPC, rpcHandler);
		
		//Topic/ConsumerGroup 
		registerHandler(Protocol.DECLARE, declareHandler); 
		registerHandler(Protocol.QUERY, queryHandler);
		registerHandler(Protocol.REMOVE, removeHandler); 
		
		//Tracker  
		registerHandler(Protocol.TRACK_PUB, trackPubServerHandler); 
		registerHandler(Protocol.TRACK_SUB, trackSubHandler); 
		registerHandler(Protocol.TRACKER, trackerHandler); 
		
		registerHandler(Protocol.SERVER, serverHandler); 
		
		
		//Monitor/Management
		registerHandler("", homeHandler);  
		registerHandler(Protocol.JS, jsHandler); 
		registerHandler(Protocol.CSS, cssHandler);
		registerHandler(Protocol.IMG, imgHandler);
		registerHandler("favicon.ico", faviconHandler);
		registerHandler(Protocol.PAGE, pageHandler);
		registerHandler(Protocol.PING, pingHandler);   
		
		registerHandler(Message.HEARTBEAT, heartbeatHandler);    
		
	}   
	
	private MessageHandler<Message> produceHandler = new MessageHandler<Message>() { 
		@Override
		public void handle(final Message msg, final Session sess) throws IOException {  
			boolean ok = validateMessage(msg,sess);
			if(!ok) return;
			
			if(!auth(msg)){ 
				ReplyKit.reply403(msg, sess);
				return;
			}
			
			final MessageQueue mq = findMQ(msg, sess);
			if(mq == null) return; 
			
			if((mq.getMask()&Protocol.MASK_RPC) != 0){ 
				if(mq.consumerCount(null) == 0){ //default consumerGroup
					ReplyKit.reply502(msg, sess);
					return;
				}
			} 
			
			final boolean ack = msg.isAck();  
			msg.removeHeader(Protocol.COMMAND);
			msg.removeHeader(Protocol.ACK);  
			mq.produce(msg);  
			
			if(ack){
				ReplyKit.reply200(msg, sess);
			}
		}
	}; 
	
	private MessageHandler<Message> rpcHandler = new MessageHandler<Message>() { 
		@Override
		public void handle(final Message msg, final Session sess) throws IOException {  
			msg.setAck(false);
			produceHandler.handle(msg, sess);
		}
	};
	
	private MessageHandler<Message> consumeHandler = new MessageHandler<Message>() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			if(!auth(msg)){ 
				ReplyKit.reply403(msg, sess);
				return;
			}
			
			MessageQueue mq = findMQ(msg, sess);
			if(mq == null) return; 
			
			mq.consume(msg, sess);  
			String topic = sess.attr(Protocol.TOPIC);
			if(!msg.getTopic().equalsIgnoreCase(topic)){
				sess.attr(Protocol.TOPIC, mq.getTopic()); //mark
				
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
			if(!auth(msg)){ 
				ReplyKit.reply403(msg, sess);
				return;
			}   
			  
			
    		MessageQueue mq = null;
    		synchronized (mqTable) {
    			mq = mqTable.get(topic);  
    			if(mq == null){ 
	    			mq = new DiskQueue(new File(config.storePath, topic));  
	    			mq.setCreator(msg.getToken()); 
	    			mqTable.put(topic, mq);
	    			log.info("MQ Created: %s", mq);
    			}
    		} 
			
			try {
				Integer topicMask = msg.getTopicMask();   
				if(topicMask != null){
					mq.setMask(topicMask);
				}
				
				String groupName = msg.getConsumeGroup();
				ConsumeGroup consumeGroup = new ConsumeGroup(msg);  
				ConsumeGroupInfo info = mq.declareGroup(consumeGroup); 
				
				if(groupName != null){  
					ReplyKit.replyJson(msg, sess, info);
					log.info("ConsumeGroup declared: %s", consumeGroup); 
				} else { 
					TopicInfo topicInfo = mq.getInfo();
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
			if(msg.getTopic() == null){  
				ReplyKit.replyJson(msg, sess, mqServer.serverInfo()); 
				return;
			} 
			
			MessageQueue mq = findMQ(msg, sess);
	    	if(mq == null){ 
	    		ReplyKit.reply404(msg, sess);
				return;
			}
	    	TopicInfo topicInfo = mq.getInfo();
	    	topicInfo.serverAddress = mqServer.getServerAddress(); 
	    	
			String group = msg.getConsumeGroup();
			if(group == null){
				ReplyKit.replyJson(msg, sess, topicInfo);
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
			
			if(!auth(msg)){ 
				ReplyKit.reply403(msg, sess);
				return;
			} 
			
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
			
			mq = mqTable.remove(mq.getTopic());
			if(mq != null){
				mq.removeTopic();
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
			String msgId = msg.getId();
			msg = new Message();
			msg.setStatus(200);
			msg.setId(msgId);
			msg.setHeader("content-type", "text/html");
			String body = FileKit.loadFileString("home.htm");
			if ("".equals(body)) {
				body = "<strong>zbus.htm file missing</strong>";
			}
			msg.setBody(body);
			sess.write(msg);
		}
	};  
	
	private Message handleTemplateRequest(String prefixPath, String url){
		Message res = new Message(); 
		if(!url.startsWith(prefixPath)){
			res.setStatus(400);
			res.setBody("Missing file name in URL"); 
			return res;
		}
		url = url.substring(prefixPath.length());   
		int idx = url.lastIndexOf('?');
		Map<String, Object> model = null;
		String fileName = url;
		if(idx >= 0){
			fileName = url.substring(0, idx); 
			String params = url.substring(idx+1);
			model = FileKit.parseKeyValuePairs(params);
		}
		String body = null;
		try{
			body = FileKit.loadTemplate(fileName, model);
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
		return res;
	}
	
	private Message handleFileRequest(String prefixPath, String url){
		Message res = new Message(); 
		if(!url.startsWith(prefixPath)){
			res.setStatus(400);
			res.setBody("Missing file name in URL"); 
			return res;
		}
		url = url.substring(prefixPath.length());   
		int idx = url.lastIndexOf('?');
		String fileName = url;
		if(idx >= 0){
			fileName = url.substring(0, idx); 
		}
		byte[] body = null;
		try{
			body = FileKit.loadFileBytes(fileName);
			if(body == null){
				res.setStatus(404);
				body = ("404: File (" + fileName +") Not Found").getBytes();
			} else {
				res.setStatus(200); 
			}
		} catch (IOException e){
			res.setStatus(404);
			body = e.getMessage().getBytes();
		}  
		res.setBody(body); 
		return res;
	}
	
	private MessageHandler<Message> pageHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException { 
			Message res = handleTemplateRequest("/page/", msg.getUrl());
			if("200".equals(res.getStatus())){
				res.setHeader("content-type", "text/html");
			}
			sess.write(res); 
		}
	};
	
	private MessageHandler<Message> jsHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = handleFileRequest("/js/", msg.getUrl());
			if(res.getStatus() == 200){
				res.setHeader("content-type", "application/javascript");
			}
			sess.write(res); 
		}
	};
	
	private MessageHandler<Message> cssHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = handleFileRequest("/css/", msg.getUrl());
			if(res.getStatus() == 200){
				res.setHeader("content-type", "text/css");
			} 
			sess.write(res);
		}
	}; 
	
	private MessageHandler<Message> imgHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = handleFileRequest("/img/", msg.getUrl());
			if(res.getStatus() == 200){
				res.setHeader("content-type", "image/svg+xml");
			} 
			sess.write(res);
		}
	}; 
	
	private MessageHandler<Message> faviconHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException {
			Message res = handleFileRequest("/img/", "/img/favicon.ico");
			if(res.getStatus() == 200){
				res.setHeader("content-type", "image/x-icon");
			} 
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
				tracker.onDownstreamNotified(event); 
				
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
			tracker.subscribe(msg, session);
		}
	}; 
	
	private MessageHandler<Message> trackerHandler = new MessageHandler<Message>() {
		
		@Override
		public void handle(Message msg, Session session) throws IOException { 
			ReplyKit.replyJson(msg, session, tracker.trackerInfo()); 
		}
	}; 
	
	private MessageHandler<Message> serverHandler = new MessageHandler<Message>() {
		public void handle(Message msg, Session sess) throws IOException { 
			ReplyKit.replyJson(msg, sess, mqServer.serverInfo());  
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
		
		tracker.cleanSubscriberSession(sess);  
	} 
	
	private boolean auth(Message msg){  
		//String token = msg.getToken(); 
		//TODO add authentication
		return true;
	}
	
    public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}  
     
   
    
	public void loadDiskQueue() throws IOException {
		log.info("Loading DiskQueues...");
		mqTable.clear();
		
		File[] mqDirs = new File(config.storePath).listFiles(new FileFilter() {
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
    
    private void handlUrlRpcMessage(Message msg){
    	// rpc/<topic>/<method>/<param_1>/../<param_n>[?module=<module>&&<header_ext_kvs>]
    	String url = msg.getUrl(); 
    	int idx = url.indexOf('?');
    	String rest = "";
    	Map<String, String> kvs = null;
    	if(idx >= 0){
    		kvs = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    		rest = url.substring(1, idx);  
    		String paramString = url.substring(idx+1); 
    		StringTokenizer st = new StringTokenizer(paramString, "&");
            while (st.hasMoreTokens()) {
                String e = st.nextToken();
                int sep = e.indexOf('=');
                if (sep >= 0) {
                	String key = e.substring(0, sep).trim().toLowerCase();
                	String val = e.substring(sep + 1).trim();  
                	kvs.put(key, val); 
                }  
            }  
    	} else {
    		rest = url.substring(1);
    	}  
    	
    	String[] bb = rest.split("/");
    	if(bb.length < 3){
    		//ignore invalid 
    		return;
    	}
    	
    	String topic = bb[1];
    	String method = bb[2];
    	msg.setTopic(topic);
    	Request req = new Request();
    	req.setMethod(method); 
    	if(kvs != null && kvs.containsKey("module")){
    		req.setModule(kvs.get("module"));
    	}
    	if(bb.length>3){
    		Object[] params = new Object[bb.length-3];
    		for(int i=0;i<params.length;i++){
    			params[i] = bb[3+i];
    		}
    		req.setParams(params); 
    	} 
    	
    	msg.setBody(JsonKit.toJSONString(req));
    }
    
    private void handleUrlMessage(Message msg){ 
    	if(msg.getCommand() != null){
    		return;
    	} 
    	String url = msg.getUrl(); 
    	if(url == null || "/".equals(url)){
    		msg.setCommand("");
    		return;
    	} 
    	int idx = url.indexOf('?');
    	String cmd = "";
    	if(idx >= 0){
    		cmd = url.substring(1, idx);  
    	} else {
    		cmd = url.substring(1);
    	} 
    	idx = cmd.indexOf('/');
    	if(idx > 0){
    		String topicGroup = cmd.substring(idx+1);
    		cmd = cmd.substring(0, idx);
    		if(restUrlCommands.contains(cmd)){
    			String[] bb = topicGroup.split("[/]"); 
    			int i = -1;
    			while(++i < bb.length){
    				if(bb[i].length() == 0) continue; 
    				String topic = bb[i];
    				if(msg.getTopic() == null){
    					msg.setTopic(topic);
    				}
    				break;
    			} 
    			while(++i < bb.length){
    				if(bb[i].length() == 0) continue;
    				String group = bb[i];
    				if(msg.getConsumeGroup() == null){
    					msg.setConsumeGroup(group);
    				}
    				break;
    			}  
    		}
    	}
    	
    	msg.setCommand(cmd.toLowerCase());
    	//handle RPC
    	if(Protocol.RPC.equalsIgnoreCase(cmd) && msg.getBody() == null){ 
    		handlUrlRpcMessage(msg); 
    	} else {
    		msg.urlToHead(); 
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
		
		if(verbose){
			log.info("\n%s", msg);
		} 
		
		
		handleUrlMessage(msg); 
		
		String cmd = msg.getCommand(); 
		
    	if(cmd != null){
    		MessageHandler<Message> handler = handlerMap.get(cmd);
	    	if(handler != null){
	    		handler.handle(msg, sess);
	    		return;
	    	}
    	}
    	
    	Message res = new Message();
    	res.setId(msg.getId()); 
    	res.setStatus(400);
    	String text = String.format("Bad format: command(%s) not support", cmd);
    	res.setBody(text); 
    	sess.write(res); 
    }  
	
    private MessageQueue findMQ(Message msg, Session sess) throws IOException{
		String topic = msg.getTopic();
		if(topic == null){
			ReplyKit.reply400(msg, sess, "Missing topic"); 
    		return null;
		}
		
		MessageQueue mq = mqTable.get(topic); 
    	if(mq == null){
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