package io.zbus.rpc.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.HttpKit;
import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.mq.MqClient;
import io.zbus.mq.MqServer;
import io.zbus.mq.Protocol;
import io.zbus.rpc.RpcProcessor;
import io.zbus.transport.Message;

public class MqRpcServer implements Closeable {
	private static final Logger logger = LoggerFactory.getLogger(MqRpcServer.class);

	private MqServer mqServer; //Only for InprocClient
	private String address;
	private String mq;
	private String mqType = Protocol.MEMORY;
	private String channel;
	private boolean authEnabled = false;
	private String apiKey = "";
	private String secretKey = "";
	
	private int clientCount = 1;
	private int heartbeatInterval = 30; // seconds
	private int poolSize = 64;

	private List<MqClient> clients = new ArrayList<>();
	private RpcProcessor processor;
	
	private ExecutorService runner;

	public MqRpcServer(RpcProcessor processor) {
		this.processor = processor;
	}
	
	public void setProcessor(RpcProcessor processor) {
		this.processor = processor;
	}

	@Override
	public void close() throws IOException {
		for(MqClient client : clients) {
			client.close();
		} 
	}
	
	public void start() {
		if(runner == null) {
			runner = Executors.newFixedThreadPool(poolSize);
		} 
		for(int i=0;i<clientCount;i++) {
			MqClient client = startClient();
			clients.add(client);
		}
	}
	
	public void syncUrlToServer() {
		if(clients.isEmpty()) return;
		
		Message req = new Message();
		req.setHeader(Protocol.CMD, Protocol.BIND); // Bind URL
		req.setHeader(Protocol.MQ, mq);
		req.setHeader(Protocol.CLEAR_BIND, true); 
		req.setBody(processor.urlEntryList(mq)); 
		
		try {
			Message res = clients.get(0).invoke(req);
			logger.info("Sync URL binds to server: "+ JsonKit.toJSONString(res));
		} catch (Exception e) {
			logger.error(e.getMessage(), e); 
		}   
	}

	protected MqClient startClient() {
		MqClient client = null;
		if (mqServer != null) {
			client = new MqClient(mqServer);
		} else if (address != null) {
			client = new MqClient(address);
		} else {
			throw new IllegalStateException("Can not create MqClient, missing address or mqServer?");
		}
		
		if (this.channel == null) this.channel = this.mq;  
		
		if(this.authEnabled) {
			client.setAuthEnabled(this.authEnabled);
			client.setApiKey(apiKey);
			client.setSecretKey(secretKey);
		}
		final MqClient mqClient = client;
		mqClient.heartbeat(heartbeatInterval, TimeUnit.SECONDS);

		mqClient.addMqHandler(mq, channel, request -> {
			String source = (String)request.getHeader(Protocol.SOURCE);
			String id = (String)request.getHeader(Protocol.ID); 
			
			String url = request.getUrl();
			if(url != null) {
				String prefix = processor.getUrlPrefix(); 
				if(!StrKit.isEmpty(prefix) && url.startsWith(prefix)) {
					url = url.substring(prefix.length());
					url = HttpKit.joinPath("/", url); 
					request.setUrl(url);
				}
			}
			
			runner.submit(()->{
				Message response = new Message(); 
				processor.process(request, response);   
				if(response.getStatus() == null) {
					response.setStatus(200);
				}
				
				response.setHeader(Protocol.CMD, Protocol.ROUTE);
				response.setHeader(Protocol.TARGET, source);
				response.setHeader(Protocol.ID, id);

				mqClient.sendMessage(response); 
			}); 
		});

		mqClient.onOpen(() -> {
			Message req = new Message();
			req.setHeader(Protocol.CMD, Protocol.CREATE); // create MQ/Channel
			req.setHeader(Protocol.MQ, mq);
			req.setHeader(Protocol.MQ_TYPE, mqType);
			req.setHeader(Protocol.CHANNEL, channel); 
			Message res = mqClient.invoke(req);
			logger.info(JsonKit.toJSONString(res));

			req = new Message();
			req.setHeader(Protocol.CMD, Protocol.SUB); // Subscribe on MQ/Channel
			req.setHeader(Protocol.MQ, mq);
			req.setHeader(Protocol.CHANNEL, channel); 
			res = mqClient.invoke(req); 
			logger.info(JsonKit.toJSONString(res));
			
			req = new Message();
			req.setHeader(Protocol.CMD, Protocol.BIND); // Bind URL
			req.setHeader(Protocol.MQ, mq);
			req.setHeader(Protocol.CLEAR_BIND, true); 
			req.setBody(processor.urlEntryList(mq));
			res = mqClient.invoke(req); 
			
			logger.info(JsonKit.toJSONString(res));
		});

		mqClient.connect();
		
		return mqClient;
	}

	public MqServer getMqServer() {
		return mqServer;
	}

	public void setMqServer(MqServer mqServer) {
		this.mqServer = mqServer;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getMq() {
		return mq;
	}

	public void setMq(String mq) {
		this.mq = mq;
	}

	public String getMqType() {
		return mqType;
	}

	public void setMqType(String mqType) {
		this.mqType = mqType;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public int getClientCount() {
		return clientCount;
	}

	public void setClientCount(int clientCount) {
		this.clientCount = clientCount;
	}

	public int getHeartbeatInterval() {
		return heartbeatInterval;
	}

	public void setHeartbeatInterval(int heartbeatInterval) {
		this.heartbeatInterval = heartbeatInterval;
	}

	public void setAuthEnabled(boolean authEnabled) {
		this.authEnabled = authEnabled;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public int getPoolSize() {
		return poolSize;
	}

	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}   
}
