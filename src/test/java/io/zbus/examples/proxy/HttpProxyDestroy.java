package io.zbus.examples.proxy;

import io.zbus.kit.ConfigKit;
import io.zbus.proxy.http.HttpProxy;
import io.zbus.proxy.http.ProxyConfig;

public class HttpProxyDestroy {

	public static void main(String[] args) throws Exception {
		String configFile = ConfigKit.option(args, "-conf", "conf/http_proxy.xml"); 
		ProxyConfig config = new ProxyConfig();
		config.loadFromXml(configFile);
		  
		HttpProxy proxy = new HttpProxy(config);
		proxy.start();
		
		proxy.close();
	}

}
