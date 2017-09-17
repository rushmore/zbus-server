package io.zbus.mq.server.auth;

import static io.zbus.kit.ConfigKit.valueOf;

import java.io.IOException;
import java.io.InputStream;

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
import io.zbus.mq.server.auth.Token.TopicResource;

public class XmlAuthProvider extends DefaultAuthProvider {   
	
	public void loadFromXml(Document doc) throws Exception{
		XPath xpath = XPathFactory.newInstance().newXPath();
		
		boolean enabled = valueOf(xpath.evaluate("/zbus/auth/@enabled", doc), false);
		tokenTable.setEnabled(enabled);
		 
		NodeList entryList = (NodeList) xpath.compile("/zbus/auth/*").evaluate(doc, XPathConstants.NODESET);
		if(entryList != null && entryList.getLength()> 0){ 
			for (int i = 0; i < entryList.getLength(); i++) {
			    Node tokenNode = entryList.item(i);    
			    Token token = new Token();
			    token.token = valueOf(xpath.evaluate("@value", tokenNode), "");   
			    token.name = valueOf(xpath.evaluate("@name", tokenNode), "");  
			    
			    String op = valueOf(xpath.evaluate("@operation", tokenNode), ""); 
			    if(op.equals("*")){
			    	token.allOperations = true; 
			    }  else {
			    	token.setOperation(op);
			    }
			    
			    
			    NodeList topicList = (NodeList) xpath.compile("./*").evaluate(tokenNode, XPathConstants.NODESET);
			    for (int j = 0; j < topicList.getLength(); j++) {
				    Node topicNode = topicList.item(j);     
				    String topic = valueOf(xpath.evaluate("@value", topicNode), ""); 
				    if(topic.equals("*")){
				    	token.allTopics = true; 
				    } 
				    TopicResource topicResource = new TopicResource();
				    topicResource.topic = topic;
				    token.topics.put(topic, topicResource);
				     
				    NodeList groupList = (NodeList) xpath.compile("./*").evaluate(topicNode, XPathConstants.NODESET);
				    for (int k = 0; k < groupList.getLength(); k++) {
				    	Node groupNode = groupList.item(k);     
					    String group = valueOf(xpath.evaluate("text()", groupNode), ""); 
					    if(group.equals("")) continue;
					    topicResource.consumeGroups.add(group);
				    } 
				    
				    if(topicResource.consumeGroups.isEmpty()){
				    	topicResource.allGroups = true; //no group set, means all groups allowded
				    } 
			    }  
			    this.tokenTable.put(token.token, token);
			} 
		}   
	}
	
	public void loadFromXml(InputStream stream) throws Exception{  
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		InputSource source = new InputSource(stream); 
		Document doc = db.parse(source); 
		loadFromXml(doc); 
	}
	
	public void loadFromXml(String configFile) { 
		InputStream stream = FileKit.inputStream(configFile);
		if(stream == null){
			throw new IllegalArgumentException(configFile + " not found");
		}
		try { 
			loadFromXml(stream); 
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

}
