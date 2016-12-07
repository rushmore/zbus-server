package io.zbus.mq.api;

/**
 * 
 * Consumer within a same ConsumeGroup, consumes the message in load-balance way.
 * 
 * 
 * 
 * @author Rushmore
 *
 */
public class Consumer extends MqAdmin{
	protected Invoker invoker;
	protected int maxInFlight = 1;
	protected boolean deleteConsumeGroupOnExit = false;
	protected boolean consumeGroupExclusive = false;
	protected String consumeGroup = "";
	
	public Consumer(Broker broker) {
		super(broker); 
	}  
	
	public Message take(int timeout){
		return null;
	}
	
	public void subscribe(String topic){
		
	}
	
	public void unsubscribe(String topic){
		
	}
	
	public void onMessage(){
		
	}
	
	public void start(){
		
	}
	
	public void route(Message message){
		
	}
}
