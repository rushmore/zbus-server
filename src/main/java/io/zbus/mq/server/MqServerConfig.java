package io.zbus.mq.server;


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

public class MqServerConfig implements Cloneable {   
	public String serverHost = "0.0.0.0";
	public int serverPort = 15555;   
	public List<ServerAddress> trackerList = new ArrayList<ServerAddress>();
	public Map<String, String> sslCertFileTable = new HashMap<String, String>();
	
	public String defaultSslCertFile;
	public String sslCertFile;
	public String sslKeyFile;
	public boolean sslEnabled;
	
	public boolean trackerOnly = false;
	public boolean verbose = false;
	public String storePath = "/tmp/zbus";  
	public String serverName;
	
	public long cleanMqInterval = 3000;       //3 seconds
	public long trackReportInterval = 30000;  //30 seconds
	
	public MqServerConfig(){ }
	
	public MqServerConfig(String configXmlFile) {
		loadFromXml(configXmlFile);
	}
	
	public void addTracker(String trackerAddress, String certPath){
		ServerAddress address = new ServerAddress(trackerAddress);
		address.sslEnabled = certPath != null;
		
		if(!trackerList.contains(address)){
			trackerList.add(address);
		}
		sslCertFileTable.put(trackerAddress, certPath);
	}
	
	public void addTracker(String trackerAddress){
		addTracker(trackerAddress, null);
	} 
	
	public List<ServerAddress> getTrackerList() {
		return trackerList;
	} 

	public String getServerHost() {
		return serverHost;
	}

	public void setServerHost(String serverHost) {
		this.serverHost = serverHost;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	} 

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public String getStorePath() {
		return storePath;
	}

	public void setStorePath(String storePath) {
		this.storePath = storePath;
	}  

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public long getCleanMqInterval() {
		return cleanMqInterval;
	}

	public void setCleanMqInterval(long cleanMqInterval) {
		this.cleanMqInterval = cleanMqInterval;
	}

	public long getTrackReportInterval() {
		return trackReportInterval;
	}

	public void setTrackReportInterval(long trackReportInterval) {
		this.trackReportInterval = trackReportInterval;
	} 
 
	public String getSslCertFile() {
		return sslCertFile;
	}

	public void setSslCertFile(String sslCertFile) {
		this.sslCertFile = sslCertFile;
	}

	public String getSslKeyFile() {
		return sslKeyFile;
	}

	public void setSslKeyFile(String sslKeyFile) {
		this.sslKeyFile = sslKeyFile;
	}

	public boolean isTrackerOnly() {
		return trackerOnly;
	}

	public void setTrackerOnly(boolean trackerOnly) {
		this.trackerOnly = trackerOnly;
	}

	public void loadFromXml(InputStream stream) throws Exception{
		XPath xpath = XPathFactory.newInstance().newXPath();     
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		InputSource source = new InputSource(stream); 
		Document doc = db.parse(source); 
		 
		this.serverHost = valueOf(xpath.evaluate("/zbus/serverHost", doc), "0.0.0.0");   
		this.serverPort = valueOf(xpath.evaluate("/zbus/serverPort", doc), 15555);
		
		this.serverName = valueOf(xpath.evaluate("/zbus/serverName", doc), null); 
		this.storePath = valueOf(xpath.evaluate("/zbus/storePath", doc), "/tmp/zbus");
		this.verbose = valueOf(xpath.evaluate("/zbus/verbose", doc), false);   
		
		this.defaultSslCertFile = valueOf(xpath.evaluate("/zbus/defaultSslCertFile", doc), null);
		this.sslEnabled = valueOf(xpath.evaluate("/zbus/sslEnabled", doc), false);
		this.sslCertFile = valueOf(xpath.evaluate("/zbus/sslEnabled/@certFile", doc), null);
		this.sslKeyFile = valueOf(xpath.evaluate("/zbus/sslEnabled/@keyFile", doc), null); 
		 
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
	}
	 
	public void loadFromXml(String configFile) { 
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
	public MqServerConfig clone() { 
		try {
			return (MqServerConfig)super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
}