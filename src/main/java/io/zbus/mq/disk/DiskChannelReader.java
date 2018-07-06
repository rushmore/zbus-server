package io.zbus.mq.disk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.kit.JsonKit;
import io.zbus.mq.Protocol;
import io.zbus.mq.Protocol.ChannelInfo;
import io.zbus.mq.disk.support.DiskMessage;
import io.zbus.mq.disk.support.QueueReader;
import io.zbus.mq.model.ChannelReader;
import io.zbus.transport.Message;

public class DiskChannelReader implements ChannelReader {
	private static final Logger logger = LoggerFactory.getLogger(DiskChannelReader.class);

	private final QueueReader reader;
	private final String name;

	public DiskChannelReader(String channel, DiskQueue diskQueue) throws IOException {
		this.name = channel;
		reader = new QueueReader(diskQueue.index, channel);
	}

	public boolean isEnd() {
		try {
			return reader.isEOF();
		} catch (IOException e) {
			return true;
		}
	}
 
	public Message read() throws IOException {
		DiskMessage data = reader.read();
		if (data == null) {
			return null;
		}
		Message res = JsonKit.parseObject(data.body, Message.class);
		res.setHeader(Protocol.OFFSET, data.offset);
		return res;
	}

	@Override
	public List<Message> read(int count) throws IOException {
		List<Message> res = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			Message data = read();
			if(data == null) break;
			res.add(data);
		}
		return res;
	}

	public boolean seek(Long offset, String checksum) throws IOException {
		return reader.seek(offset, checksum);
	}

	@Override
	public String getFilter() {
		return reader.getFilter();
	}

	public void setFilter(String filter) {
		reader.setFilter(filter);
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}

	public Integer getMask() {
		return reader.getMask();
	}

	public void setMask(Integer mask) {
		reader.setMask(mask);
	}

	@Override
	public ChannelInfo info() {
		ChannelInfo info = new ChannelInfo();
		info.name = name;
		info.mask = getMask();
		info.filter = getFilter();
		info.offset = reader.getTotalOffset(); 
		return info;
	}

	@Override
	public void destroy() {
		try {
			reader.delete();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
}