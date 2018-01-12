package io.zbus.proxy.tcp;

import static io.zbus.kit.ConfigKit.valueOf;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.zbus.kit.ConfigKit.XmlConfig;
import io.zbus.kit.NetKit;

public class TcpProxyConfig extends XmlConfig{ 
	
	private List<ProxyEntry> entries = new ArrayList<ProxyEntry>(); 
	
	public void loadFromXml(Document doc) throws Exception{
		XPath xpath = XPathFactory.newInstance().newXPath();    
		  
		NodeList entryList = (NodeList) xpath.compile("/zbus/tcpProxy/*").evaluate(doc, XPathConstants.NODESET);
		if(entryList != null && entryList.getLength()> 0){ 
			for (int i = 0; i < entryList.getLength(); i++) {
			    Node node = entryList.item(i);    
			    ProxyEntry entry = new ProxyEntry();
			    
			    String server = xpath.evaluate("./server", node);  
				
				entry.targetAddress = xpath.evaluate("./target", node); 
				entry.connectTimeout = valueOf(xpath.evaluate("./connectTimeout", node), 3000);
				entry.idleTimeout = valueOf(xpath.evaluate("./idleTimeout", node), 60000); 
			   
			    Object[] hp = NetKit.hostPort(server);
			    entry.proxyHost = (String)hp[0];
			    entry.proxyPort = (Integer)hp[1];  
			    
			    entries.add(entry);
			}
		}    
	} 
	
	public List<ProxyEntry> getEntries() {
		return entries;
	}
	
	public void setEntries(List<ProxyEntry> entries) {
		this.entries = entries;
	} 
	
	public static class ProxyEntry{
		public String proxyHost = "0.0.0.0"; 
		public int proxyPort = 80; 
		public String targetAddress;      //IP:Port
		public int connectTimeout = 3000; //milliseconds
		public int idleTimeout = 60000;   //milliseconds 
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
}