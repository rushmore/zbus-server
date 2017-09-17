/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.zbus.mq;
 

import static io.zbus.mq.Protocol.ACK;
import static io.zbus.mq.Protocol.COMMAND;
import static io.zbus.mq.Protocol.CONSUME_GROUP;
import static io.zbus.mq.Protocol.CONSUME_WINDOW;
import static io.zbus.mq.Protocol.GROUP_FILTER;
import static io.zbus.mq.Protocol.GROUP_MASK;
import static io.zbus.mq.Protocol.GROUP_START_COPY;
import static io.zbus.mq.Protocol.GROUP_START_MSGID;
import static io.zbus.mq.Protocol.GROUP_START_OFFSET;
import static io.zbus.mq.Protocol.GROUP_START_TIME;
import static io.zbus.mq.Protocol.HOST;
import static io.zbus.mq.Protocol.OFFSET;
import static io.zbus.mq.Protocol.ORIGIN_ID;
import static io.zbus.mq.Protocol.ORIGIN_STATUS;
import static io.zbus.mq.Protocol.ORIGIN_URL;
import static io.zbus.mq.Protocol.RECVER;
import static io.zbus.mq.Protocol.SENDER;
import static io.zbus.mq.Protocol.TAG;
import static io.zbus.mq.Protocol.TOKEN;
import static io.zbus.mq.Protocol.TOPIC;
import static io.zbus.mq.Protocol.TOPIC_MASK;
import static io.zbus.mq.Protocol.VERSION;

import io.zbus.mq.server.Fix;
 

public class Message extends io.zbus.transport.http.Message {    
	
	public Message() {
	}
	
	public Message(io.zbus.transport.http.Message raw){
		super(raw); 
	}
	
	public String getVersion(){
		return this.getHeader(VERSION); 
	} 
	
	public Message setVersion(String value) {
		this.setHeader(VERSION, value);
		return this;
	} 
	
	public String getCommand() { 
		return this.getHeader(COMMAND);
	} 
	public Message setCommand(String value) {
		this.setHeader(COMMAND, value); 
		return this;
	}   
	
	public String getHost(){
		return this.getHeader(HOST);
	}  
	public void setHost(String value){
		this.setHeader(HOST, value);
	} 
	
	public String getSender() {
		return this.getHeader(SENDER);
	} 
	public Message setSender(String value) {
		this.setHeader(SENDER, value);
		return this;
	}
	
	
	public String getReceiver() {
		return this.getHeader(RECVER);
	} 
	public Message setReceiver(String value) {
		this.setHeader(RECVER, value);
		return this;
	} 
	
	public String getToken() {
		return this.getHeader(TOKEN);
	} 
	public Message setToken(String value) {
		this.setHeader(TOKEN, value);
		return this;
	}   
	
	public String getTag() {
		return this.getHeader(TAG);
	} 
	public Message setTag(String value) {
		this.setHeader(TAG, value);
		return this;
	}   
	
	public String getRemoteAddr() {
		return this.getHeader(REMOTE_ADDR);
	} 
	public Message setRemoteAddr(String value) {
		this.setHeader(REMOTE_ADDR, value);
		return this;
	}  
	
	
	public Integer getOriginStatus() {
		String value = this.getHeader(ORIGIN_STATUS);
		if(value != null){
			return Integer.valueOf(value);
		}
		
		return Fix.getOriginStatus(this); 
	} 
	
	public Message setOriginStatus(Integer value) {
		this.setHeader(ORIGIN_STATUS, value);
		Fix.setOriginStatus(this, value);
		return this;
	}  
	
	public String getOriginUrl() {
		String value = this.getHeader(ORIGIN_URL); 
		if(value == null) value = Fix.getOriginUrl(this);    
		return value;
	} 
	
	public Message setOriginUrl(String value) {
		this.setHeader(ORIGIN_URL, value);
		Fix.setOriginUrl(this, value);
		return this;
	}   
	
	public String getOriginId() {
		String value = this.getHeader(ORIGIN_ID); 
		if(value == null) value = Fix.getOriginId(this);  
		return value;
	} 
	
	public Message setOriginId(String value) { 
		this.setHeader(ORIGIN_ID, value); 
		Fix.setOriginId(this, value);
		return this;
	} 
	
	public boolean isAck() {
		String ack = this.getHeader(ACK);
		if(ack == null) return true; //default to true
		ack = ack.trim().toLowerCase();
		return ack.equals("1") || ack.equals("true");
	} 
	
	public void setAck(boolean ack){
		String value = ack? "1":"0";
		this.setHeader(ACK, value);
	} 
	
	public String getTopic(){
		String topic = getHeader(Protocol.TOPIC);
		if(topic != null) return topic;
		
		return Fix.getTopic(this); 
	} 
	public Message setTopic(String value) {
		this.setHeader(TOPIC, value);
		return this;
	} 
	
	public String getConsumeGroup(){
		String value = this.getHeader(CONSUME_GROUP);
		return value;
	} 
	public Message setConsumeGroup(String value) {
		this.setHeader(CONSUME_GROUP, value);
		return this;
	} 
	public Long getOffset(){
		String value = this.getHeader(OFFSET);
		if(value == null) return null;
		return Long.valueOf(value);
	} 
	public Message setOffset(Long value) {
		this.setHeader(OFFSET, value);
		return this;
	} 
	
	public Integer getTopicMask(){
		String value = getHeader(TOPIC_MASK);
		if(value != null) return Integer.valueOf(value);
		
		return Fix.getTopicMask(this); 
	} 
	
	public Message setTopicMask(Integer value) { 
		this.setHeader(TOPIC_MASK, value);
		return this;
	} 
	
	public Integer getGroupMask(){
		String value = this.getHeader(GROUP_MASK);
		if(value == null) return null;
		return Integer.valueOf(value);
	} 
	public Message setGroupMask(Integer value) {
		this.setHeader(GROUP_MASK, value);
		return this;
	} 
	
	public Long getGroupStartOffset(){
		String value = this.getHeader(GROUP_START_OFFSET);
		if(value == null) return null;
		return Long.valueOf(value);
	} 
	public Message setGroupStartOffset(Long value) {
		this.setHeader(GROUP_START_OFFSET, value);
		return this;
	}   
	public Long getGroupStartTime(){
		String value = this.getHeader(GROUP_START_TIME);
		if(value == null) return null;
		return Long.valueOf(value);
	} 
	public Message setGroupStartTime(Long value) {
		this.setHeader(GROUP_START_TIME, value);
		return this;
	}  
	public String getGroupStartCopy(){
		return this.getHeader(GROUP_START_COPY);
	} 
	public Message setGroupStartCopy(String value) {
		this.setHeader(GROUP_START_COPY, value);
		return this;
	}  
	
	public String getGroupStartMsgId(){
		String value = this.getHeader(GROUP_START_MSGID);
		return value;
	} 
	public Message setGroupStartMsgId(String mq) {
		this.setHeader(GROUP_START_MSGID, mq);
		return this;
	} 
	
	public Integer getConsumeWindow(){
		String value = this.getHeader(CONSUME_WINDOW);
		if(value == null) return null;
		return Integer.valueOf(value);
	} 
	
	public Message setConsumeWindow(Integer value) {
		this.setHeader(CONSUME_WINDOW, value);
		return this;
	}  
	public String getGroupFilter() {
		return getHeader(GROUP_FILTER);
	} 
	
	public Message setGroupFilter(String value) {
		this.setHeader(GROUP_FILTER, value);
		return this;
	}    
	
	public static Message parse(byte[] data){
		io.zbus.transport.http.Message message = io.zbus.transport.http.Message.parse(data);
		return new Message(message);
	}
	
	public static Message copyWithoutBody(Message msg){
		io.zbus.transport.http.Message res = io.zbus.transport.http.Message.copyWithoutBody(msg);
		return new Message(res);
	}
}