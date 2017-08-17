package io.zbus.mq;

import java.io.IOException;

import io.zbus.mq.Protocol.ConsumeGroupInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.transport.Session;

public class MemoryQueue implements MessageQueue {

	@Override
	public void produce(Message message) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public Message consume(String consumeGroup, Integer window) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void consume(Message message, Session session) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public ConsumeGroupInfo declareGroup(ConsumeGroup consumeGroup) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeGroup(String groupName) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeTopic() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public int consumerCount(String consumeGroup) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void cleanSession(Session sess) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getTopic() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TopicInfo getInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getUpdateTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getCreator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCreator(String value) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getMask() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setMask(int value) {
		// TODO Auto-generated method stub

	}

}
