package io.zbus.unittests.mq.server.auth;

import io.zbus.mq.server.auth.XmlAuthProvider;

public class XmlAuthProviderTest {

	public static void main(String[] args) { 
		XmlAuthProvider auth = new XmlAuthProvider();
		auth.loadFromXml("conf/zbus1.xml"); 
		System.err.println(auth);
	}

}
