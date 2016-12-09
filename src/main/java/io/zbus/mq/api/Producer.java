package io.zbus.mq.api;

public class Producer extends MqAdmin{ 
	
	public Producer(Broker broker) {
		super(broker); 
	} 
	
	/**
	 * Automatically do batch if enabled
	 * 
	 * Construct a hierarchy message to make a batch message with transaction enabled.
	 * 
	 * @param message
	 * @return
	 */
	public Message send(Message message){  
		fillCtrlFileds(message);
		
		return broker.invoke(message);
	} 
	
	private void fillCtrlFileds(Message message){ 
		message.setCmd("produce");
		
		if(message.getTopic() == null){
			message.setTopic(topic);
		}

		if(message.getAppId() == null){
			message.setAppId(appId);
		}
		if(message.getToken() == null){
			message.setToken(token);
		}
	} 

}