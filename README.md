                /\\\       
                \/\\\        
                 \/\\\    
     /\\\\\\\\\\\ \/\\\         /\\\    /\\\  /\\\\\\\\\\     
     \///////\\\/  \/\\\\\\\\\  \/\\\   \/\\\ \/\\\//////     
           /\\\/    \/\\\////\\\ \/\\\   \/\\\ \/\\\\\\\\\\    
          /\\\/      \/\\\  \/\\\ \/\\\   \/\\\ \////////\\\  
         /\\\\\\\\\\\ \/\\\\\\\\\  \//\\\\\\\\\   /\\\\\\\\\\  
         \///////////  \/////////    \/////////   \//////////       QQ Group: 467741880

# ZBUS = MQ + RPC  
zbus strives to make Message Queue and Remote Procedure Call fast, light-weighted and easy to build your own service-oriented architecture for many different platforms. Simply put, zbus = mq + rpc.

zbus carefully designed on its protocol and components to embrace KISS(Keep It Simple and Stupid) principle, but in all it delivers power and elasticity. 

## Features
- Fast MQ on disk, capable of unicast, multicast and broadcast messaging models
- Easy RPC support out of box, language agnostic
- Officially support Java/.NET/Javascript/C_C++/Python/Go/PHP clients
- Extremely light-weighted
- High Availability inside, able to join or leave any distributed components
- TCP/HTTP/WebSocket, Monitor, all in one port, support DMZ deployment
- Based on simple protocol: HTTP-header extension, and browser access friendly

## Clients

[zbus-java](https://gitee.com/rushmore/zbus)

[zbus-dotnet](https://gitee.com/rushmore/zbus-dotnet)

[zbus-javascript](https://gitee.com/rushmore/zbus-javascript)

[zbus-python](https://gitee.com/rushmore/zbus-python)

[zbus-php](https://gitee.com/rushmore/zbus-php)

[zbus-cpp](https://gitee.com/rushmore/zbus-cpp)

[zbus-go](https://gitee.com/rushmore/zbus-go)


## Getting started 
In zbus-dist directory, just run zbus.bat/sh, JDK6+ required.

Incase you may interest in the client projects, go to zbus source root directory

	git submodule update --init --recursive  


Maven

	<dependency>
		<groupId>io.zbus</groupId>
		<artifactId>zbus</artifactId>
		<version>0.9.0</version>
	</dependency>

## Performance
Fast performance test (Apache Benchmark)

	Declare MQ: http://localhost:15555/declare/MyTopic
	
	Produce 
	ab -k -c 32 -n 1000000 http://localhost:15555/produce/MyTopic
	Consume 
	ab -k -c 32 -n 1000000 http://localhost:15555/consume/MyTopic

	Single Mac i7 box with SSD 
	Produce:  ~80,000/s
	Consume:  ~90,000/s
	RPC InProc: ~60,000/s




## MQ and RPC at a glance

In the monitor page [http://localhost:15555](http://localhost:15555) , as you can see, there are two sections
![Monitor](/doc/monitor.png?raw=true "Monitor")
1. Tracked Servers

	List all the MqServer joined.
	
	zbus instance by default plays the role of both Tracker and MqServer, and if tracker mode enabled, the zbus instance is able to accept other zbus instances to join as tracked server. And the tracked items such topics are aggregated for viewing.

	To join zbus tracker(s), just start zbus with --tracker={address_list}

2. TopicTable

	List all Topic avaiable for the tracked MqServers
	
	Including important fields as Topic name, message depth in disk, consume-groups, consumer online, message consume filter.

	For more detailed explanation of these glossaries please refer to components section.

In zbus, RPC is implemented via MQ, a MQ can be a message container for the RPC service, and internally it contains a mask to indicate the MQ's purpose of RPC.

The monitor page keeps evolving, more and more items will be added in.

## Components

![Archit](/doc/archit.png?raw=true "Archit")

The above figure shows the 3 major distributed components--**broker**, **producer** and **consumer** 

**Broker** 

*Single mode*, Broker is the zbus instance(MqServer), stores messages produced, delivers messages to consumers.

*HA mode*, Broker is an abstracted server(Trackers + MqServers), capable of failover and smart algorithms on selection of MqServer, 
but still just work like a single MqServer.


**Producer**

Producer publish message to Broker, capable of customerize the selection algorithm based on the application's will.

**Consumer**

Consumer takes message from Broker, capable of relocating consuming position based on message offset.

**Topic**

Topic is a message queue identity, message with same topic fall into the same message store queue. 

**ConsumeGroup**

ConsumeGroup controls the consumer behavior, it stores latest consumed position of the topic, and filter out message if ConsumeGroup's filter is set.
ConsumeGroup is super light-weighted in zbus, it is just like a pointer to the message queue, with carefully craft on ConsumeGroup,
applications can form unicast, multicast, and broadcast messaging models.

## Protocol


## Internal Design

Broker Design

DiskQueue Design

RPC Design




 