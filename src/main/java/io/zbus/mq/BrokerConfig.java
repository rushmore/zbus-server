package io.zbus.mq;


import static io.zbus.kit.ConfigKit.valueOf;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import io.zbus.kit.FileKit;
import io.zbus.kit.StrKit;
import io.zbus.transport.ServerAddress;

public class BrokerConfig implements Cloneable {   
	public List<ServerAddress> trackerList = new ArrayList<ServerAddress>(); 
	public Map<String, String> sslCertFileTable = new HashMap<String, String>(); 
	public String defaultSslCertFile;  
	public int clientPoolSize = 32; 
	
	public BrokerConfig(){}
	  
	public BrokerConfig(String configXmlFile) { 
		 loadFromXml(configXmlFile);
	} 
	
	public BrokerConfig setTrackerList(String trackerList){ 
		if(trackerList == null) return this;
		
		String[] addresses = trackerList.split("[, ;]");
		for(String address : addresses){
			if(StrKit.isEmpty(address)) continue;
			address = address.trim(); 
			addTracker(address);
		}
		return this;
	} 
	
	public void addTracker(ServerAddress trackerAddress){
		if(!trackerList.contains(trackerAddress)){
			trackerList.add(trackerAddress);
		}
	}
	
	public void addTracker(String trackerAddress, String certPath){
		ServerAddress address = new ServerAddress(trackerAddress); 
		if(certPath != null){
			address.sslEnabled = true;
			sslCertFileTable.put(trackerAddress, certPath);
		}
		
		if(!trackerList.contains(address)){
			trackerList.add(address);
		}  
	} 
	
	public void addTracker(String trackerAddress){
		addTracker(trackerAddress, null);
	} 
	  
	
	public void addSslCertFile(String address, String certPath){
		sslCertFileTable.put(address, certPath);
	}
	
	public List<ServerAddress> getTrackerList() {
		return trackerList;
	}    
	public Map<String, String> getSslCertFileTable() {
		return sslCertFileTable;
	}

	public void setSslCertFileTable(Map<String, String> sslCertFileTable) {
		this.sslCertFileTable = sslCertFileTable;
	}

	public String getDefaultSslCertFile() {
		return defaultSslCertFile;
	}

	public void setDefaultSslCertFile(String defaultSslCertFile) {
		this.defaultSslCertFile = defaultSslCertFile;
	}

	public int getClientPoolSize() {
		return clientPoolSize;
	}

	public void setClientPoolSize(int clientPoolSize) {
		this.clientPoolSize = clientPoolSize;
	}

	public void setTrackerList(List<ServerAddress> trackerList) {
		this.trackerList = trackerList;
	}

	public void loadFromXml(InputStream stream) throws Exception{
		XPath xpath = XPathFactory.newInstance().newXPath();     
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		InputSource source = new InputSource(stream);  
		Document doc = db.parse(source);  
		
		this.defaultSslCertFile = valueOf(xpath.evaluate("/zbus/defaultSslCertFile", doc), null); 
		 
		NodeList list = (NodeList) xpath.compile("/zbus/trackerList/*").evaluate(doc, XPathConstants.NODESET);
		if(list != null && list.getLength()> 0){ 
			for (int i = 0; i < list.getLength(); i++) {
			    Node node = list.item(i);    
			    String address = xpath.evaluate("address", node);
			    String sslEnabled = xpath.evaluate("sslEnabled", node);  
			    String certFile = xpath.evaluate("sslEnabled/@certFile", node);  
			    if(StrKit.isEmpty(address)) continue; 
			    
			    ServerAddress serverAddress = new ServerAddress(address, valueOf(sslEnabled, false));
			    if(!StrKit.isEmpty(certFile)){ 
			    	sslCertFileTable.put(address, certFile);
			    } 
			    trackerList.add(serverAddress); 
			}
		}    
		
		list = (NodeList) xpath.compile("/zbus/sslCertFileTable/*").evaluate(doc, XPathConstants.NODESET);
		if(list != null && list.getLength()> 0){ 
			for (int i = 0; i < list.getLength(); i++) {
			    Node node = list.item(i);    
			    String address = xpath.evaluate("sslCertFile/@address", node); 
			    String certFile = xpath.evaluate("sslCertFile/@certFile", node);  
			    if(StrKit.isEmpty(address)) continue; 
			     
			    if(!StrKit.isEmpty(certFile)){ 
			    	sslCertFileTable.put(address, certFile);
			    }  
			}
		}   
	}
	 
	public void loadFromXml(String configFile){ 
		InputStream stream = FileKit.inputStream(configFile);
		if(stream == null){
			throw new IllegalArgumentException(configFile + " not found");
		}
		try { 
			loadFromXml(stream);
			stream.close();
		} catch (Exception e) { 
			throw new IllegalArgumentException(configFile + " load error", e);
		} finally {
			if(stream != null){
				try {
					stream.close();
				} catch (IOException e) {
					//ignore
				}
			}
		}
	} 
	 
	
	@Override
	public BrokerConfig clone() { 
		try {
			return (BrokerConfig)super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	} 
}