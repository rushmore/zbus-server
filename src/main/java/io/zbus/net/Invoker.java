package io.zbus.net;

import java.io.Closeable;

public interface Invoker<REQ extends Identifiable, RES extends Identifiable> extends Closeable{ 
	Future<RES> invoke(REQ message);  
}
