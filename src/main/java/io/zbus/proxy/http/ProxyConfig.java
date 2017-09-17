package io.zbus.proxy.http;

import static io.zbus.kit.ConfigKit.valueOf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.zbus.kit.ClassKit;
import io.zbus.kit.ConfigKit.XmlConfig;
import io.zbus.mq.Broker;

public class ProxyConfig extends XmlConfig { 
	private Broker broker; 
	private String brokerAddress;
	private int connectionCount = 4; //Number of connections to zbus broker per consumer 
	private int consumeTimeout = 10000;
	private String token;
	private Map<String, ProxyEntry> entryTable = new HashMap<String, ProxyEntry>(); 

	public static class ProxyEntry {
		public String topic;
		public String token;
		public MessageFilter sendFilter;
		public MessageFilter recvFilter;
		public List<String> targetList = new ArrayList<String>();
	} 
	
	public static class ProxyHandlerConfig{
		public String topic;
		public String targetUrl;
		
		public String token;
		public Broker broker; 
		public int connectionCount;
		public int consumeTimeout;
		
		public MessageFilter sendFilter;
		public MessageFilter recvFilter;
	}

	public void loadFromXml(Document doc) throws Exception{
		XPath xpath = XPathFactory.newInstance().newXPath();  
		this.brokerAddress = valueOf(xpath.evaluate("/zbus/httpProxy/@zbus", doc), "localhost:15555");  
		this.connectionCount = valueOf(xpath.evaluate("/zbus/httpProxy/@connectionCount", doc), 4);   
		this.consumeTimeout = valueOf(xpath.evaluate("/zbus/httpProxy/@consumeTimeout", doc), 10000);   
		this.token = valueOf(xpath.evaluate("/zbus/httpProxy/@token", doc), null);   
		 
		NodeList entryList = (NodeList) xpath.compile("/zbus/httpProxy/*").evaluate(doc, XPathConstants.NODESET);
		if(entryList != null && entryList.getLength()> 0){ 
			for (int i = 0; i < entryList.getLength(); i++) {
			    Node node = entryList.item(i);    
			    ProxyEntry entry = new ProxyEntry();
			    String entryName = valueOf(xpath.evaluate("@entry", node), ""); 
			    entry.token = valueOf(xpath.evaluate("@token", node), ""); 
			    if (entryName.equals("")) continue;
			    
			    String sendFilterClass = valueOf(xpath.evaluate("@sendFilter", node), ""); 
			    String recvFilterClass = valueOf(xpath.evaluate("@recvFilter", node), ""); 
			    if(!sendFilterClass.equals("")){
			    	entry.sendFilter = ClassKit.newInstance(sendFilterClass); 
			    }
			    if(!recvFilterClass.equals("")){ 
			    	entry.recvFilter = ClassKit.newInstance(recvFilterClass);
			    }
			    
			    entry.topic = entryName;
			    
			    NodeList targetList = (NodeList) xpath.compile("./*").evaluate(node, XPathConstants.NODESET);
			    for (int j = 0; j < targetList.getLength(); j++) {
				    Node targetNode = targetList.item(j);    
				    String target = valueOf(xpath.evaluate("text()", targetNode), ""); 
				    entry.targetList.add(target);
			    } 
			    
			    this.entryTable.put(entryName, entry);
			}
		}   
	}
	

	public Broker getBroker() {
		return broker;
	}

	public int getConnectionCount() {
		return connectionCount;
	} 
 
	public void setBroker(Broker broker) {
		this.broker = broker;
	}

	public void setConnectionCount(int connectionCount) {
		this.connectionCount = connectionCount;
	} 
	
	public String getBrokerAddress() {
		return brokerAddress;
	}

	public void setBrokerAddress(String brokerAddress) {
		this.brokerAddress = brokerAddress;
	} 

	public Map<String, ProxyEntry> getEntryTable() {
		return entryTable;
	}

	public void setEntryTable(Map<String, ProxyEntry> entryTable) {
		this.entryTable = entryTable;
	} 

	public int getConsumeTimeout() {
		return consumeTimeout;
	} 

	public void setConsumeTimeout(int consumeTimeout) {
		this.consumeTimeout = consumeTimeout;
	} 

	public String getToken() {
		return token;
	} 
	
	public void setToken(String token) {
		this.token = token;
	}  
}