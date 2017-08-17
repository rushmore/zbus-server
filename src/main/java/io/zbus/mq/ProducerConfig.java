package io.zbus.mq;

import io.zbus.mq.Broker.ServerSelector;

public class ProducerConfig extends MqConfig {   
	protected ServerSelector produceServerSelector; 
	
	public ProducerConfig(){
		
	}
	
	public ProducerConfig(Broker broker){
		super(broker);
	}
	
	public ServerSelector getProduceServerSelector() {
		return produceServerSelector;
	} 

	public void setProduceServerSelector(ServerSelector produceServerSelector) {
		this.produceServerSelector = produceServerSelector;
	} 
	@Override
	public ProducerConfig clone() { 
		return (ProducerConfig)super.clone();
	}
	
}
