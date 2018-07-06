package io.zbus.auth;

import static io.zbus.kit.ConfigKit.valueOf;

import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.zbus.kit.ConfigKit.XmlConfig;

public class XmlApiKeyProvider extends XmlConfig implements ApiKeyProvider {  
	private Map<String, String> table = new HashMap<>(); 
	private String authXPath = "/zbus/auth";
	
	public void setAuthXPath(String authXPath) {
		this.authXPath = authXPath;
	}
	
	public XmlApiKeyProvider() { 
	}
	
	public XmlApiKeyProvider(String configXmlFile) {
		loadFromXml(configXmlFile);
	}

	@Override
	public void loadFromXml(Document doc) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList list = (NodeList) xpath.compile(authXPath+"/*").evaluate(doc, XPathConstants.NODESET);
		if (list == null || list.getLength() <= 0)
			return;
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i); 
			String apiKey = valueOf(xpath.evaluate("apiKey", node), null); 
			String secretKey = valueOf(xpath.evaluate("secretKey", node), null); 
			if(apiKey == null || secretKey == null) continue;
			
			table.put(apiKey, secretKey);
		}
	} 

	@Override
	public String secretKey(String apiKey) { 
		return table.get(apiKey);
	}

	@Override
	public boolean apiKeyExists(String apiKey) { 
		return table.containsKey(apiKey);
	}  
}
