package io.zbus.mq;
 
import io.zbus.mq.Broker.ServerSelector;

public class ConsumerConfig extends MqConfig {  
	protected String topic;
	protected ConsumeGroup consumeGroup; 
	protected Integer consumeWindow; 
	protected int consumeTimeout = 120000;// 2 minutes  
	
	protected MessageHandler messageHandler;   
	protected int connectionCount = 4;
	protected int consumeRunnerPoolSize = 64; 
	protected int maxInFlightMessage = 100;
	
	protected ServerSelector consumeServerSelector; 
	
	public ConsumerConfig(){
		
	}
	
	public ConsumerConfig(Broker broker){
		super(broker);
	}
	
	public ConsumeGroup getConsumeGroup() {
		return consumeGroup;
	} 
	
	public void setConsumeGroup(ConsumeGroup consumerGroup) {
		this.consumeGroup = consumerGroup;
	} 
	
	public void setConsumeGroup(String group) {
		setConsumeGroup(group, null);
	} 
	
	public void setConsumeGroup(String group, String msgFilter) {
		ConsumeGroup consumerGroup = new ConsumeGroup();
		consumerGroup.setGroupName(group);
		consumerGroup.setFilter(msgFilter);
		this.consumeGroup = consumerGroup;
	} 
	

	public Integer getConsumeWindow() {
		return consumeWindow;
	} 

	public void setConsumeWindow(Integer consumeWindow) {
		this.consumeWindow = consumeWindow;
	} 

	public int getConsumeTimeout() {
		return consumeTimeout;
	} 

	public void setConsumeTimeout(int consumeTimeout) {
		this.consumeTimeout = consumeTimeout;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	} 

	public ServerSelector getConsumeServerSelector() {
		return consumeServerSelector;
	}

	public void setConsumeServerSelector(ServerSelector consumeServerSelector) {
		this.consumeServerSelector = consumeServerSelector;
	} 

	public MessageHandler getMessageHandler() {
		return messageHandler;
	}

	public void setMessageHandler(MessageHandler messageHandler) {
		this.messageHandler = messageHandler;
	} 
	
	public int getConsumeRunnerPoolSize() {
		return consumeRunnerPoolSize;
	}

	public void setConsumeRunnerPoolSize(int consumeRunnerPoolSize) {
		this.consumeRunnerPoolSize = consumeRunnerPoolSize;
	}

	public int getMaxInFlightMessage() {
		return maxInFlightMessage;
	}

	public void setMaxInFlightMessage(int maxInFlightMessage) {
		this.maxInFlightMessage = maxInFlightMessage;
	} 

	public int getConnectionCount() {
		return connectionCount;
	}

	public void setConnectionCount(int connectionCount) {
		this.connectionCount = connectionCount;
	}

	@Override
	public ConsumerConfig clone() { 
		return (ConsumerConfig)super.clone();
	}
	
}
