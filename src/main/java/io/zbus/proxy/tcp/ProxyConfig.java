package io.zbus.proxy.tcp;

import static io.zbus.kit.ConfigKit.valueOf;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import io.zbus.kit.ConfigKit.XmlConfig;
import io.zbus.kit.StrKit;

public class ProxyConfig extends XmlConfig{
	private String proxyHost = "0.0.0.0"; 
	private int proxyPort = 80; 
	private String targetAddress;      //IP:Port
	private int connectTimeout = 3000; //milliseconds
	private int idleTimeout = 60000;   //milliseconds 
	
	public void loadFromXml(Document doc) throws Exception{
		XPath xpath = XPathFactory.newInstance().newXPath();    
		  
		String server = xpath.evaluate("/zbus/tcpProxy/server", doc); 
		this.targetAddress = xpath.evaluate("/zbus/tcpProxy/target", doc); 
	    this.connectTimeout = valueOf(xpath.evaluate("/zbus/tcpProxy/connectTimeout", doc), 3000);
	    this.idleTimeout = valueOf(xpath.evaluate("/zbus/tcpProxy/idleTimeout", doc), 60000); 
	   
	    Object[] hp = StrKit.hostPort(server);
	    this.proxyHost = (String)hp[0];
	    this.proxyPort = (Integer)hp[1];  
	} 
	
	public String getProxyHost() {
		return proxyHost;
	}
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}
	public int getProxyPort() {
		return proxyPort;
	}
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}
	public String getTargetAddress() {
		return targetAddress;
	}
	public void setTargetAddress(String targetAddress) {
		this.targetAddress = targetAddress;
	}
	public int getConnectTimeout() {
		return connectTimeout;
	}
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}
	public int getIdleTimeout() {
		return idleTimeout;
	}
	public void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}  

}