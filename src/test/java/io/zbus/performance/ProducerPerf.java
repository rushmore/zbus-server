package io.zbus.performance;
 

import io.zbus.kit.ConfigKit;
import io.zbus.mq.Broker;
import io.zbus.mq.BrokerConfig;
import io.zbus.mq.Message;
import io.zbus.mq.Producer;
import io.zbus.mq.ProducerConfig;

/**
 * 
 * For simple produce, AB test is better tool.
 * 
 * @author Rushmore
 *
 */
public class ProducerPerf {
	public static void main(String[] args) throws Exception{   
		final String serverAddress = ConfigKit.option(args, "-b", "127.0.0.1:15555");
		final int threadCount = ConfigKit.option(args, "-c", 16); 
		final int loopCount = ConfigKit.option(args, "-loop", 1000000);
		final int logCount = ConfigKit.option(args, "-log", 10000);
		final String topic = ConfigKit.option(args, "-topic", "MyTopic"); 
		
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setTrackerList(serverAddress);
		brokerConfig.setClientPoolSize(threadCount);
		Broker broker = new Broker(brokerConfig);
		
		final ProducerConfig config = new ProducerConfig(); 
		config.setBroker(broker);  
		
		Perf perf = new Perf(){ 
			
			@Override
			public TaskInThread buildTaskInThread() {
				return new TaskInThread(){
					Producer producer = new Producer(config); 
					
					@Override
					public void initTask() throws Exception {
						producer.declareTopic(topic);
					}
					
					@Override
					public void doTask() throws Exception {
						Message msg = new Message(); 
						msg.setTopic(topic);
						msg.setBody("hello world"); 
						msg = producer.publish(msg);
					}
				};
			}  
		}; 
		
		perf.loopCount = loopCount;
		perf.threadCount = threadCount;
		perf.logInterval = logCount;
		perf.run();
		
		perf.close();
		broker.close();
	} 
}
