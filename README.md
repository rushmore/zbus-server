#ZBUS = MQ + RPC  

Zbus strives to make Message Queue and Remote Procedure Call fast, light-weighted and easy to build your own elastic and micro-service oriented bus for many different platforms. Simply put, ZBUS = MQ + RPC.

QQ Discussion: **467741880**  
Star this project if you like zbus:)

##1. Features

- Fast Message Queue in persisted mode, capable of Unicast, Multicast and Broadcast messaging models.
- Language agnostic RPC support out of box.
- High Availability inside, easy to add more servers.
- Simple HTTP-alike control protocol(Browser direct control), easy to extend clients.
- TCP/HTTP/WebSocket, all in one single port, including monitor, capable of DMZ deployment.
- Multiple platforms support, JAVA/.NET/JavaScript/Python/C_C++/GO/PHP... 
- Extremely light-weighted(<400K + a very few dependencies like netty.jar)

 
##2. Architecture


![Archit](https://git.oschina.net/uploads/images/2017/0517/183402_0efce626_7458.png "Archit")

Producer = Broker = Consumer


![MQ](https://git.oschina.net/uploads/images/2017/0517/183644_a160de3b_7458.png "MQ")


 
###2.1 Topic

 
###2.2 ConsumeGroup

ConsumeGroup plays an important role of MQ in zbus. It is mainly designed to support different messaging models in applications.

Regarding to a topic, no matter how many consume-groups exists, there is only one copy of messages queue storing messages received from Producer(s), Consumer(s) can read the message out from topic via a single consume-group or multiple ones, Consumers and consume-groups combination helps to build different messaging models, typically including Unicast, Broadcast, and Multicast.

- Unicast -- Consumers share only one consume-group, message in topic is designed to be consumed by only one consumer
- Broadcast -- Consumers use privately owned consume-group, message in topic is then consumed by every consumer
- Multicast -- Multiple groups of Consumers, in each group, consumers share one same consume-group.

 
###2.3 MqClient

MqClient is a TCP based client to control topic and consume-group in MqServer:

- produce message to MqServer as Producer role
- consume message from MqServer as Consumer role

- declare create/update topic and consume-group, capable of change consuming policy
- query topic and consume-group
- remove topic and consume-group 
- empty topic and consume-group

 
###2.4 MqServer


 
###2.5 Broker

Producer-Broker-Consumer PBC model is the high level view of a Messaging Queue system.
Broker refers to a MQ server, however, from the client point of view, Broker in client is an abstraction 
of connection(pooling) to MqServer(s).

Broker manages a set of MqClientPools each of which connects to a MqServer in group, and all ,
creating the RouteTable of topics. 

Broker also detects the connectivity of remote MqServers, removes the dead ones.

Broker can work in dynamic or static way
- dynamic, configure with tracker address(or list of trackers)
- static, just manual call addServer.

Broker defers the MqServer selection algorithm to Producer and Consumer, both of which could be configured
with special selection algorithm.

 
**BrokerRouteTable**

	serverTable: serverAddressKey => ServerInfo
	topicTable: topicName => [TopicInfo List]
	votesTable: serverAddressKey => [Voted TrackerServer List]

	+ updateVotes(trackerInfo)
		trackerInfo = {
			serverAddress: {address: xx, sslEnabled: xxx}
			trackedServerList: [{address: xx1, sslEnabled: xxx},{address: xx2, sslEnabled: xxx}]
		}
		
		1. for each trackedServer in trackedServerlist:
			  add(no changes if exists) vote with trackerAddress
		2. for each server in votesTable: //remove server not in this tracker's tracking list
			  votedTrackerSet = votesTable[server] 
			  if server not in trackerInfo.trackedServerList:
					votedTrackerSet.remove(trackerAddress)
		3. rebuild topicTable
	
		+ addServer(serverInfo)
			rebuild topicTable after added of serverInfo
		+ removeServer(serverAddress)
			rebuild topicTable after removal of serverInfo
 


##3. Protocol

*Common headers*

	cmd: <cmd>
	topic: <topic> 
	token: [token]

**produce**
	
	cmd: produce
	tag: [tag]    //tag of message, used for consume-group filter
	body: [body]

**consume**
	
	cmd: consume
	consume_group: [group_name]
	consume_window: [window_size] //default to null, means 1

**declare**

	cmd: declare
	consume_group: [consume-group name] //short name=> group
	
	topic_mask:    [topic mask value]    
	group_mask:    [consume-group mask value]
	group_filter:  [message filter for group] //filter on message's tag
	
	//locate the group's start point
	group_start_copy:   [consume-group name] //copy from
	group_start_time:   [consume-group start time]
	group_start_offset: [consume-group start offset]
	group_start_msgid:  [consume-group start offset's msgid] //validate for offset value


**query**

	cmd: query
	topic: [topic] //if not set, result is the server info
	consume_group: [consume-group]

**remove**
	
	cmd: remove
	consume_group: [consume-group] //if not set, remove whole topic including groups belonging to the topic

**empty**
	
	cmd: empty
	consume_group: [consume-group] //if not set, empty whole topic including groups belonging to the topic


To be browser friendly, URL request and parameters are parsed if header key-value not populated, however,
header key-value always take precedence over URL parse result.

	URL Pattern: /<cmd>/[topic]/[group]/[?k=v list]
	
	/produce/topic
	/consume/topic/[group]
	/declare/topic/[group]
	/remove/topic/[group] 
	/empty/topic/[group]
	
	/query/[topic]/[group]  * topic can be optional in order to query whole server
	
	/track_sub
	/track_pub
	
	/rpc/topic/method/arg1/arg2.../[?module=xxx&&appid=xxx&&token=xxx]  *exception: rpc not follow 


Exampels:

	http://localhost:15555/consume/MyTopic  consume one message from topic=MyTopic, consume-group default to same name as topic name
	http://localhost:15555/consume/MyTopic/group1   consume one message from group1 in MyTopic
	http://localhost:15555/declare/MyTopic   declare a topic named MyTopic, consume-group with same MyTopic should be created as well.
	http://localhost:15555/declare/MyTopic/group1   declare a topic named MyTopic, and consume-group named group1.
	http://localhost:15555/declare/MyTopic/group1?group_filter=abc&&group_mask=16   same as above, but with consume-group filter set to abc, consume-group mask set to 16
	http://localhost:15555/query/MyTopic  query topic info named MyTopic
	http://localhost:15555/query/MyTopic/group1  query consume-group info named group1 in MyTopic
	http://localhost:15555/remove/MyTopic  remove topic named MyTopic, which will remove all the consume-groups included
	http://localhost:15555/remove/MyTopic/group1  remove consume-group named group1 in MyTopic
	
	http://localhost:15555/rpc/myrpc/plus/1/2  invoke remote method plus with parameter 1 and 2, remote service registered as myrpc topic



  

##4. Monitoring

![Monitor](https://git.oschina.net/uploads/images/2017/0517/184806_39bb1fc9_7458.png "Monitor")
 