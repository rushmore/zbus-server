package io.zbus.transport;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.zbus.auth.RequestSign;
import io.zbus.transport.http.WebsocketClient;
import io.zbus.transport.inproc.InprocClient;

/**
 * 
 * Decoration pattern on AbastractClient, making Client's sub class type adaptive to all real clients such as
 * WebsocketClient, InprocClient
 * 
 * @author leiming.hong Jun 27, 2018
 *
 */
public class Client extends AbastractClient {
	protected AbastractClient support;
	
	public Client(String address) {
		support = new WebsocketClient(address);
	}

	public Client(IoAdaptor ioAdaptor) {
		support = new InprocClient(ioAdaptor);
	} 
	
	@Override
	protected void sendMessage0(Message data) { 
		support.sendMessage0(data);
	}
	
	public void sendMessage(Message data) {
		support.sendMessage(data);
	}

	public void connect() {
		support.connect();
	}

	public synchronized void heartbeat(long interval, TimeUnit timeUnit, AbastractClient.MessageBuilder builder) {
		support.heartbeat(interval, timeUnit, builder);
	}

	@Override
	public void close() throws IOException {
		support.close();
	}

	public void invoke(Message req, DataHandler<Message> dataHandler) {
		support.invoke(req, dataHandler);
	}

	public void invoke(Message req, DataHandler<Message> dataHandler,
			ErrorHandler errorHandler) {
		support.invoke(req, dataHandler, errorHandler);
	}

	public Message invoke(Message req) throws IOException, InterruptedException {
		return support.invoke(req);
	}

	public Message invoke(Message req, long timeout, TimeUnit timeUnit)
			throws IOException, InterruptedException {
		return support.invoke(req, timeout, timeUnit);
	}

	public boolean handleInvokeResponse(Message response) throws Exception {
		return support.handleInvokeResponse(response);
	};

	public void setApiKey(String apiKey) {
		support.setApiKey(apiKey);
	}

	public void setSecretKey(String secretKey) {
		support.setSecretKey(secretKey);
	}

	public void setAuthEnabled(boolean authEnabled) {
		support.setAuthEnabled(authEnabled);
	}

	public void setRequestSign(RequestSign requestSign) {
		support.setRequestSign(requestSign);
	}

	public void onMessage(DataHandler<Message> onMessage) {
		support.onMessage(onMessage);
	}

	public void onClose(EventHandler onClose) {
		support.onClose(onClose);
	}

	public void onOpen(EventHandler onOpen) {
		support.onOpen(onOpen);
	}

	public void onError(ErrorHandler onError) {
		support.onError(onError);
	}

	public void setReconnectDelay(int reconnectDelay) {
		support.setReconnectDelay(reconnectDelay);
	}
	
	@Override
	public void setAfterReceived(MessageInterceptor afterReceived) { 
		support.setAfterReceived(afterReceived);
	}
	
	@Override
	public void setBeforeSend(MessageInterceptor beforeSend) { 
		support.setBeforeSend(beforeSend);
	}
}