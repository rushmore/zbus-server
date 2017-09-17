package io.zbus.mq.server.auth;

import io.zbus.kit.StrKit;
import io.zbus.mq.Message;
import io.zbus.mq.Protocol;
import io.zbus.mq.server.auth.Token.TopicResource;

/**
 * DefaultAuthProvider authenticates on Token's Operation(cmd) and Resource(topic/consume_group)
 * 
 * Subclass may only need to load the TokenTable, such as from database or file system. 
 * 
 * @author Rushmore
 *
 */
public class DefaultAuthProvider implements AuthProvider {  
	protected TokenTable tokenTable = new TokenTable(); //default to empty, disabled
	
	@Override
	public boolean auth(Message message) {   
		if(!tokenTable.isEnabled()){ 
			return true;
		}
		String tokenStr = message.getToken();
		if(tokenStr == null){ //treat null as ""
			tokenStr = "";
		}
		
		Token token = tokenTable.get(tokenStr);
		if(token == null) { //No token found
			return false;
		}
		 
		if(Operation.isEnabled(token.operation, Operation.ADMIN)){ //no need to check resource
			return true;
		}  	
		
		String cmd = message.getCommand(); 
		if(!authOperation(cmd, token)) return false;
		 
		return authResource(message, token); 
	}   
	
	@Override
	public Token getToken(String token) { 
		if(!tokenTable.isEnabled()){
			return Token.ALLOW; 
		}
		//token not set, default to empty
		if(token == null) token = "";
		return tokenTable.get(token);
	}
	
	@Override
	public void addToken(Token token) {
		tokenTable.put(token.token, token); 
	} 
	
	@Override
	public void setEnabled(boolean enabled) {
		tokenTable.setEnabled(enabled);
	}
	
	public boolean authOperation(String cmd, Token token){ 
		if(token.allOperations) return true;
		Operation op = Operation.find(cmd);
		if(op == null) return true; //command not found, no need to auth
		
		return Operation.isEnabled(token.operation, op); 
	} 
	
	public boolean authResource(Message message, Token token){ 
		if(token.allTopics) return true; 
		String topic = message.getTopic();
		if(StrKit.isEmpty(topic)) return true; //no need to check
		
		TopicResource topicResource = token.topics.get(topic);
		if(topicResource == null){ //topic not in token's list
			return false;
		} 
		 
		if(topicResource.allGroups) return true;  
		
		String cmd = message.getCommand();
		if(!needCheckConsumeGroup(cmd)) return true; //some commands like produce, no need to check
		
		String consumeGroup = message.getConsumeGroup();
		if(StrKit.isEmpty(consumeGroup)){ 
			consumeGroup = topic;
		}
		
		if(!topicResource.consumeGroups.contains(consumeGroup)) return false; 
		
		return true; 
	} 
	
	protected boolean needCheckConsumeGroup(String cmd){
		if(Protocol.PRODUCE.equals(cmd)) return false; //shortcut
		
		if(Protocol.CONSUME.equals(cmd)) return true;
		if(Protocol.UNCONSUME.equals(cmd)) return true;
		if(Protocol.DECLARE.equals(cmd)) return true;
		if(Protocol.REMOVE.equals(cmd)) return true;
		if(Protocol.EMPTY.equals(cmd)) return true;
		
		//otherwise, no need to check by default
		return false;
	}  
}
