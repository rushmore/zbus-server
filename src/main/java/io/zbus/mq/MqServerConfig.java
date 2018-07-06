package io.zbus.mq;

import static io.zbus.kit.ConfigKit.valueOf;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import io.zbus.auth.DefaultAuth;
import io.zbus.auth.RequestAuth;
import io.zbus.auth.XmlApiKeyProvider;
import io.zbus.kit.ConfigKit.XmlConfig;
import io.zbus.mq.plugin.UrlFilter;

public class MqServerConfig extends XmlConfig { 
	public ServerConfig publicServer;
	public ServerConfig privateServer;
	public ServerConfig monitorServer;
	
	public int maxSocketCount = 102400;
	public int packageSizeLimit = 1024 * 1024 * 64; // 64M
	public String mqDiskDir = "/tmp/zbus"; 
	public String mqDbUrl;  
	public boolean verbose = true; 
	public UrlFilter urlFilter;  //null to use default
	
	public MqServerConfig() { 
		
	}
	
	public MqServerConfig(String host, int port) {
		this.publicServer = new ServerConfig(host+":"+port);
	}

	public MqServerConfig(String configXmlFile) {
		loadFromXml(configXmlFile);
	} 
	
	private ServerConfig loadConfig(Document doc, XPath xpath, String serverName) throws Exception { 
		String path = String.format("/zbus/%s/address", serverName);
		String address = valueOf(xpath.evaluate(path, doc), null); 
		if(address == null) return null;
		if(address.equals("")) return null;
		
		ServerConfig config = new ServerConfig();
		config.address = address;
		config.sslEnabled = valueOf(xpath.evaluate("/zbus/"+serverName+"/sslEnabled", doc), false);
		config.sslCertFile = valueOf(xpath.evaluate("/zbus/"+serverName+"/sslEnabled/@certFile", doc), null);
		config.sslKeyFile = valueOf(xpath.evaluate("/zbus/"+serverName+"/sslEnabled/@keyFile", doc), null);
		
		String authXPath = "/zbus/"+serverName+"/auth";
		if (valueOf(xpath.evaluate(authXPath, doc), null) != null) {
			XmlApiKeyProvider provider = new XmlApiKeyProvider();
			provider.setAuthXPath(authXPath);
			provider.loadFromXml(doc);
			config.auth = new DefaultAuth(provider); 
		}
		return config;
	}

	@Override
	public void loadFromXml(Document doc) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();

		this.publicServer = loadConfig(doc, xpath, "public");
		this.privateServer = loadConfig(doc, xpath, "private");
		this.monitorServer = loadConfig(doc, xpath, "monitor"); 

		this.maxSocketCount = valueOf(xpath.evaluate("/zbus/maxSocketCount", doc), 102400);
		String size = valueOf(xpath.evaluate("/zbus/packageSizeLimit", doc), "64M");
		size = size.toUpperCase();
		if (size.endsWith("M")) {
			this.packageSizeLimit = Integer.valueOf(size.substring(0, size.length() - 1)) * 1024 * 1024;
		} else if (size.endsWith("G")) {
			this.packageSizeLimit = Integer.valueOf(size.substring(0, size.length() - 1)) * 1024 * 1024 * 1024;
		} else {
			this.packageSizeLimit = Integer.valueOf(size);
		} 
		this.verbose = valueOf(xpath.evaluate("/zbus/verbose", doc), true);
	}

	public ServerConfig getPublicServer() {
		return publicServer;
	}

	public void setPublicServer(ServerConfig publicServer) {
		this.publicServer = publicServer;
	}

	public ServerConfig getPrivateServer() {
		return privateServer;
	}

	public void setPrivateServer(ServerConfig privateServer) {
		this.privateServer = privateServer;
	}

	public ServerConfig getMonitorServer() {
		return monitorServer;
	}

	public void setMonitorServer(ServerConfig monitorServer) {
		this.monitorServer = monitorServer;
	}

	public int getMaxSocketCount() {
		return maxSocketCount;
	}

	public void setMaxSocketCount(int maxSocketCount) {
		this.maxSocketCount = maxSocketCount;
	}

	public int getPackageSizeLimit() {
		return packageSizeLimit;
	}

	public void setPackageSizeLimit(int packageSizeLimit) {
		this.packageSizeLimit = packageSizeLimit;
	}

	public String getMqDiskDir() {
		return mqDiskDir;
	}

	public void setMqDiskDir(String mqDiskDir) {
		this.mqDiskDir = mqDiskDir;
	}

	public String getMqDbUrl() {
		return mqDbUrl;
	}

	public void setMqDbUrl(String mqDbUrl) {
		this.mqDbUrl = mqDbUrl;
	}
	
 
	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}  

	public UrlFilter getUrlFilter() {
		return urlFilter;
	}

	public void setUrlFilter(UrlFilter urlFilter) {
		this.urlFilter = urlFilter;
	}



	public static class ServerConfig{
		public String address;
		public boolean sslEnabled = false;
		public String sslCertFile;
		public String sslKeyFile;
		public RequestAuth auth;
		
		public ServerConfig() {
			
		}
		
		public ServerConfig(String address) {
			this.address = address;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public boolean isSslEnabled() {
			return sslEnabled;
		}

		public void setSslEnabled(boolean sslEnabled) {
			this.sslEnabled = sslEnabled;
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

		public RequestAuth getAuth() {
			return auth;
		}

		public void setAuth(RequestAuth auth) {
			this.auth = auth;
		}
		
	}
}
