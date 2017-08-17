package io.zbus.transport;

import java.io.IOException;

public interface MessageHandler<T> { 
	void handle(T msg, Session session) throws IOException;   
}