# zbus project 
## Do NOT use it in production, still Alpha
Goal:
1. Lightweighted
2. High Performance
3. Backward Compatible
4. Fully tested

ZBUS-0.8.0

@since 0.8.0

io.zbus.mq.api
Message
Producer/ZbusProducer
Consumer/ZbusConsumer
Broker/ZbusBroker
BlockingQueue (JDK)/ZbusRemoteQueue | ZbusLocalQueue

io.zbus.rpc.api
Request/Response
RpcInvoker/RpcService

io.zbus.mq -- Message Queue
io.zbus.rpc -- Remote Procedure Call
io.zbus.ha -- High Availability
io.zbus.net -- Networking

zbus-dotnet-client
zbus-javascript-client (WebSocket/NodeJS)
zbus-c-client

zbus-python-client

zbus-php-client

zbus-msmq
zbus-kcxp
zbus-webservice

io.zbus.mq.api
io.zbus.rpc.api

io.zbus.mq
io.zbus.rpc

Admin
+ Topic: Create/Delete, Pause/Resume, SignalQuit2Consumers
+ ConsumeGroup: Pause/Resume, SkipToEnd, SetToTime, Delete, Create
+ AppId+Token security

Message
  +Cmd
  +Topic
  +Group
  +AppId
  +Token
  +Ack
  +Tag
  +Id
  +Sender
  +Receiver
  +Window(1)
  +BatchSize (1)
  +BatchInTx (true|false)
  +Status(200|400|403|404|500)

  - attr(name, value)
  - setBody(byte[] body)

MqAdmin
  + declareTopic(String topic, Map<String, Object> params)
  + removeTopic(String topic)
  + queryTopic(String topic)

CreateTopic
+ topic => <topic_name>
+ appid => <app_id>
+ token => <token>
+ <key> => <value>

Invoker
+ invoke(msg)
+ invokeAsync(msg)

Broker : Invoker
+ getInvoker(msg)
+ closeInvoker(inovker)
+ onServerJoin()
+ onSeverLeave()
+ workingServers()
+ selectServer(SelectionAlgorithm sa)

TopicMetadata
+ topic
+ createdTime
+ lastProducedTime
+ lastConsumedTime
+ activeConsumerCount
+ messageAccumulated

ConsumeGroupMetadata
+ topic
+ groupId
+ lastConsumedTime
+ activeConsumerCount
+ exclusive
+ deleteOnExit
+ dileveryTag

Producer (Topic, AppId, Token)
  + put(msg)

Consumer (Topic, AppId, Token)
  + take(timeout)
  + onMessage(ConsumerMessageHandler)
  + start()

ConsumerService
+

Model

Topic |||||||||||||
  ConsumeGroup1 <-----
  ConsumeGroup2 <-----
  ConsumeGroup3 <-----
            ^Tag

ConsumeGroup start from:
1. 0-offset from Topic history
2. latest offset from Topic
3. timestamp offset from Topic
4. n-offset from Topic history

ConsumeGroup type:
1. deleteOnExit(must be exclusive)
2. exclusive(only one instance of consumer allowed)
3. shared

Message Disk format
0. offset: 8
1. timestamp: 8
2. msgid: 16
3. length: 4
4. ctrl: 1
5. tag: 256 [len+data]
6. binary body

Search message by, for tracing
1. offset
2. [timestamp range] + msgid

Topic: MyTopic

Rules for Tag:

  # can only be at last

Tag: abc.* 
abc match only abc

abc.* match abc.x, abc.xyz

abc.# match only abc.xyz, abc.def.mnp, ....

abc.*.xyz

*.abc.*

*.abc.#

Producer
=======================
P ==> B (Produce)
  
* [R] cmd = produce        
* [R] topic = {topic}       
* [R] id = {id}            
* [O] appid = {appid}       
* [O] token = {token}       
* [O] tag = {tag}           
* [O] ack = true/false    
* [O] batch_size = {batch_size}
* [O] batch_in_tx = {batch_in_tx}   
  

B ==> P (OnAck)
* [R] cmd = ack
* [R] id = {id}

B ==> P (OnData)
* [R] cmd = data
* [R] id = {id}

Consumer
=======================
C ==> B (Consume)
* [R] cmd = consume
* [R] topic = {topic}
* [O] consume_group = {group}
* [O] window = {int}
* [O] offset_ack = msg_offset

B ==> C (OnData)

* [R] cmd = data
* [R] id = {id}

B ==> C (OnCtrl)
* [R] cmd = exit

Admin
============================
appid+token => topic list

  cmd=declare_topic
  appid=<appid>
  token=<token>
  topic_name=<topic>
  topic_ctrl=<ctrl_int>

  cmd=delete_topic
  cmd=query_topic

  cmd=declare_consume_group
  cmd=delete_consume_group
  cmd=query_consume_group