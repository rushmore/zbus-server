package io.zbus.transport;

import java.io.IOException;

public class CompositeClient<REQ extends Id, RES extends Id> implements Client<REQ, RES>{
	
	protected Client<REQ, RES> support;  
	
	public CompositeClient(){
		
	}
	
	public CompositeClient(Client<REQ, RES> support){
		this.support = support;
	}
	
	@Override
	public RES invokeSync(REQ req, long timeout) throws IOException, InterruptedException { 
		return support.invokeSync(req, timeout);
	} 
	

	@Override
	public RES invokeSync(REQ req) throws IOException, InterruptedException { 
		return support.invokeSync(req);
	}

	@Override
	public void invokeAsync(REQ req, ResultCallback<RES> callback) throws IOException {
		support.invokeAsync(req, callback);
	}

	@Override
	public void sessionCreated(Session sess) throws IOException {
		support.sessionCreated(sess);
	}

	@Override
	public void sessionToDestroy(Session sess) throws IOException {
		support.sessionToDestroy(sess);
	}

	@Override
	public void onMessage(Object msg, Session sess) throws IOException {
		support.onMessage(msg, sess);
	}

	@Override
	public void onError(Throwable e, Session sess) throws Exception {
		support.onError(e, sess);
		
	}

	@Override
	public void onIdle(Session sess) throws IOException {
		support.onIdle(sess);
	}

	@Override
	public void close() throws IOException {
		support.close();
	}

	@Override
	public boolean hasConnected() {
		return support.hasConnected();
	}

	@Override
	public void connectAsync() throws IOException {
		support.connectAsync();
		
	}

	@Override
	public void connectSync(long timeout) throws IOException, InterruptedException {
		support.connectSync(timeout);
	}

	@Override
	public void ensureConnectedAsync() {
		support.ensureConnectedAsync();
	}

	@Override
	public void sendMessage(REQ req) throws IOException, InterruptedException {
		support.sendMessage(req);
	}

	@Override
	public void onMessage(io.zbus.transport.MessageHandler<RES> msgHandler) {
		support.onMessage(msgHandler);
	}

	@Override
	public void onError(io.zbus.transport.Client.ErrorHandler errorHandler) {
		support.onError(errorHandler);
	}

	@Override
	public void onConnected(io.zbus.transport.Client.ConnectedHandler connectedHandler) {
		support.onConnected(connectedHandler);
	}

	@Override
	public void onDisconnected(io.zbus.transport.Client.DisconnectedHandler disconnectedHandler) {
		support.onDisconnected(disconnectedHandler);
	} 
	
	@Override
	public <V> V attr(String key) {
		return support.attr(key);
	}

	@Override
	public <V> void attr(String key, V value) {
		support.attr(key, value);
	}
 
}
 
