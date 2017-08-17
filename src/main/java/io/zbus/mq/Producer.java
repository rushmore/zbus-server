package io.zbus.mq;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import io.zbus.mq.Broker.ServerSelector;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.transport.ResultCallback;
import io.zbus.transport.ServerAddress;
 

public class Producer extends MqAdmin{  
	private ServerSelector produceServerSelector;  
	
	public Producer(ProducerConfig config){
		super(config); 
		
		this.produceServerSelector = config.getProduceServerSelector();
		if(this.produceServerSelector == null){
			this.produceServerSelector = new DefaultProduceServerSelector();
		} 
	}
	
	public Producer(Broker broker){
		this(new ProducerConfig(broker));
	}
	
	public Message publish(Message msg, int timeout) throws IOException, InterruptedException {
		MqClientPool[] poolArray = broker.selectClient(this.produceServerSelector, msg);
		if(poolArray.length < 1){
			throw new MqException("Missing MqClient for publishing message: " + msg);
		}
		MqClientPool pool = poolArray[0]; 
		MqClient client = null;
		try {
			client = pool.borrowClient();  
			return configClient(client).produce(msg, timeout);
		} finally {
			pool.returnClient(client);
		} 
	}   
	
	public Message publish(Message msg) throws IOException, InterruptedException {
		return publish(msg, invokeTimeout);
	}
	
	public void publishAsync(Message msg, ResultCallback<Message> callback) throws IOException {
		MqClientPool[] poolArray = broker.selectClient(this.produceServerSelector, msg);
		if(poolArray.length < 1){
			throw new MqException("Missing MqClient for publishing message: " + msg);
		}
		MqClientPool pool = poolArray[0]; 
		MqClient client = null;
		try {
			client = pool.borrowClient();
			configClient(client).produceAsync(msg, callback);
		} finally {
			pool.returnClient(client);
		} 
	}   
	
	public ServerSelector getProduceServerSelector() {
		return produceServerSelector;
	}

	public void setProduceServerSelector(ServerSelector produceServerSelector) {
		this.produceServerSelector = produceServerSelector;
	}



	public class DefaultProduceServerSelector implements ServerSelector{ 
		@Override
		public ServerAddress[] select(BrokerRouteTable table, Message message) { 
			int serverCount = table.serverTable().size();
			if (serverCount == 0) {
				return new ServerAddress[0];
			}
			String topic = message.getTopic();
			if(topic == null){
				return new ServerAddress[0];
			}
			Map<ServerAddress, TopicInfo> topicServerTable = table.topicTable().get(topic);
			if (topicServerTable == null || topicServerTable.size() == 0) {
				return new ServerAddress[0];
			} 
			TopicInfo target = null;
			for(Entry<ServerAddress, TopicInfo> e : topicServerTable.entrySet()){
				TopicInfo current = e.getValue();
				if(target == null){
					target = current;
					continue;
				} 
				if (target.consumerCount < current.consumerCount) { //consumer count decides
					target = current;
				}
			} 
			return new ServerAddress[]{target.serverAddress};
		} 
	} 
}
