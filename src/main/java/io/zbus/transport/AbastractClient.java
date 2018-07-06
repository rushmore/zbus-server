package io.zbus.transport;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.auth.DefaultSign;
import io.zbus.auth.RequestSign;
import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit; 
/**
 * The common base class of all Client types.
 * 
 * It consumes <code>Message</code> type, provides main functionalities of following.
 * <p> sync and async way invoking, message matching by callback table on Id of message coming and forth between server and client.
 * <p> authentication intercepter on <code>Message</code> headers.
 * 
 * @author leiming.hong Jun 27, 2018
 *
 */
public abstract class AbastractClient implements Closeable {
	private static final Logger logger = LoggerFactory.getLogger(AbastractClient.class);

	protected String apiKey;
	protected String secretKey;
	protected boolean authEnabled = false;
	protected RequestSign requestSign = new DefaultSign();

	protected DataHandler<Message> onMessage;
	protected EventHandler onClose;
	protected EventHandler onOpen;
	protected ErrorHandler onError; 
	
	protected MessageInterceptor beforeSend;
	protected MessageInterceptor afterReceived;

	protected int reconnectDelay = 3000; // 3s

	protected Map<String, AbastractClient.RequestContext> callbackTable = new ConcurrentHashMap<>(); // id->context
	protected ScheduledExecutorService runner = Executors.newScheduledThreadPool(4);

	public AbastractClient() {
		onMessage = msg -> {
			if(afterReceived != null) {
				afterReceived.intercept(msg);
			}
			
			handleInvokeResponse(msg);
		};

		onClose = () -> {
			try {
				Thread.sleep(reconnectDelay); // TODO make it async?
			} catch (InterruptedException e) {
				// ignore
			}
			connect();
		};

		onError = e -> {
			;
			if (onClose != null) {
				try {
					onClose.handle();
				} catch (Exception ex) {
					logger.error(ex.getMessage(), ex);
				}
			}
		};
	}

	protected abstract void sendMessage0(Message data);
	
	public void sendMessage(Message data) {
		if(beforeSend != null) {
			beforeSend.intercept(data);
		} 
		if (authEnabled) {
			if (apiKey == null) {
				throw new IllegalStateException("apiKey not set");
			}
			if (secretKey == null) {
				throw new IllegalStateException("secretKey not set");
			}

			requestSign.sign(data, apiKey, secretKey);
		}
		sendMessage0(data);
	}

	public void connect() {

	}

	public synchronized void heartbeat(long interval, TimeUnit timeUnit, AbastractClient.MessageBuilder builder) {
		runner.scheduleAtFixedRate(() -> {
			Message msg = builder.build();
			sendMessage(msg);
		}, interval, interval, timeUnit); 
	}

	@Override
	public void close() throws IOException {
		onClose = null;
		onError = null;

		if (runner != null) {
			runner.shutdown();
		}
	}

	public void invoke(Message req, DataHandler<Message> dataHandler) {
		invoke(req, dataHandler, null);
	}

	public void invoke(Message req, DataHandler<Message> dataHandler,
			ErrorHandler errorHandler) {

		String id = StrKit.uuid();
		req.setHeader(Message.ID, id); 
		AbastractClient.RequestContext ctx = new RequestContext(req, dataHandler, errorHandler);
		callbackTable.put(id, ctx);

		sendMessage(req);
	}

	public Message invoke(Message req) throws IOException, InterruptedException {
		return invoke(req, 10, TimeUnit.SECONDS);
	}

	public Message invoke(Message req, long timeout, TimeUnit timeUnit)
			throws IOException, InterruptedException {
		CountDownLatch countDown = new CountDownLatch(1);
		AtomicReference<Message> res = new AtomicReference<Message>();
		long start = System.currentTimeMillis();
		invoke(req, data -> {
			res.set(data);
			countDown.countDown();
		});
		countDown.await(timeout, timeUnit);
		if (res.get() == null) {
			long end = System.currentTimeMillis();
			String id = (String) req.getHeader(Message.ID);
			String msg = String.format("Timeout(Time=%dms, ID=%s): %s", (end - start), id, JsonKit.toJSONString(req));
			throw new IOException(msg);
		}
		return res.get();
	}

	public boolean handleInvokeResponse(Message response) throws Exception {
		String id = (String) response.getHeader(Message.ID);
		if (id != null) {
			AbastractClient.RequestContext ctx = callbackTable.remove(id);
			if (ctx != null) { // 1) Request-Response invocation
				if (ctx.onData != null) {
					ctx.onData.handle(response);
				} else {
					logger.warn("Missing handler for: " + response);
				} 
				return true;
			}
		}
		return false;
	};

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public void setAuthEnabled(boolean authEnabled) {
		this.authEnabled = authEnabled;
	}

	public void setRequestSign(RequestSign requestSign) {
		this.requestSign = requestSign;
	}

	public void onMessage(DataHandler<Message> onMessage) {
		this.onMessage = onMessage;
	}

	public void onClose(EventHandler onClose) {
		this.onClose = onClose;
	}

	public void onOpen(EventHandler onOpen) {
		this.onOpen = onOpen;
	}

	public void onError(ErrorHandler onError) {
		this.onError = onError;
	}

	public void setReconnectDelay(int reconnectDelay) {
		this.reconnectDelay = reconnectDelay;
	}
	
	public void setAfterReceived(MessageInterceptor afterReceived) {
		this.afterReceived = afterReceived;
	}
	
	public void setBeforeSend(MessageInterceptor beforeSend) {
		this.beforeSend = beforeSend;
	}

	public static class RequestContext {
		public Message request;
		public DataHandler<Message> onData;
		public ErrorHandler onError;

		RequestContext(Message request, DataHandler<Message> onData, ErrorHandler onError) {
			this.request = request;
			this.onData = onData;
			this.onError = onError;
		}
	}

	public static interface MessageBuilder {
		Message build();
	}
}