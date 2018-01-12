package io.zbus.mq;


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
import io.zbus.kit.FileKit;
import io.zbus.kit.StrKit;
import io.zbus.transport.ServerAddress;

public class BrokerConfig extends XmlConfig {   
	private List<ServerAddress> trackerList = new ArrayList<ServerAddress>();  
	private int clientPoolSize = 32;   
	private int readyTimeout = 3000; 
	
	public BrokerConfig(){
		
	}
	  
	public BrokerConfig(String configXmlFile) { 
		 loadFromXml(configXmlFile);
	} 
	
	public void loadFromXml(Document doc) throws Exception{ 
		XPath xpath = XPathFactory.newInstance().newXPath();  
		this.readyTimeout = valueOf(xpath.evaluate("/zbus/readyTimeout", doc), 3000); 
		this.clientPoolSize = valueOf(xpath.evaluate("/zbus/clientPoolSize", doc), 32); 
		
		NodeList list = (NodeList) xpath.compile("/zbus/trackerList/*").evaluate(doc, XPathConstants.NODESET);
		if(list != null && list.getLength()> 0){ 
			for (int i = 0; i < list.getLength(); i++) {
			    Node node = list.item(i);    
			    String address = xpath.evaluate("address", node);
			    String sslEnabled = xpath.evaluate("sslEnabled", node);  
			    String certFile = xpath.evaluate("sslEnabled/@certFile", node);  
			    if(StrKit.isEmpty(address)) continue; 
			    
			    ServerAddress serverAddress = new ServerAddress(address, valueOf(sslEnabled, false));
			    if(serverAddress.isSslEnabled()){
			    	String certificate = FileKit.loadFile(certFile);
			    	serverAddress.setCertificate(certificate);
			    }
			    trackerList.add(serverAddress); 
			}
		}    
	} 
	
	
	public void addTracker(ServerAddress trackerAddress){
		if(!trackerList.contains(trackerAddress)){
			trackerList.add(trackerAddress);
		}
	} 
	
	public void addTracker(String trackerAddress){ 
		ServerAddress serverAddress = new ServerAddress(trackerAddress);
		addTracker(serverAddress);
	}  
	 
	public List<ServerAddress> getTrackerList() {
		return trackerList;
	}   

	public void setTrackerList(List<ServerAddress> trackerList) {
		this.trackerList = trackerList;
	}  
	
	public BrokerConfig setTrackerList(String trackerList){ 
		if(trackerList == null) return this; 
		String[] addresses = trackerList.split("[, ;]");
		for(String address : addresses){
			address = address.trim(); 
			if(StrKit.isEmpty(address)) continue; 
			addTracker(address);
		}
		return this;
	} 
	
	public int getClientPoolSize() {
		return clientPoolSize;
	}

	public void setClientPoolSize(int clientPoolSize) {
		this.clientPoolSize = clientPoolSize;
	} 

	public int getReadyTimeout() {
		return readyTimeout;
	}

	public void setReadyTimeout(int readyTimeout) {
		this.readyTimeout = readyTimeout;
	} 
}