# zbus project 
## Do NOT use it in production, still Alpha
Goal:
1. Lightweighted
2. High Performance
3. Backward Compatible
4. Fully tested
5. Message trace friendly 

zbus-dotnet-client
zbus-c-client
zbus-python-client

zbus-javascript-client (WebSocket/NodeJS)
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
+ Channel: Pause/Resume, SkipToEnd, SetToTime, Delete, Create
+ AppId+Token security

Message
  +Cmd
  +Topic
  +Channel
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

  - get/setHeader(name, value)
  - get/setBody(byte[] body)

Model
Topic |||||||||||||
  Channel1<-----
  Channel2<-----
  Channel3<-----
       ^Tag
Channel--ConsumeGroup

Channel start from:
1. 0-offset from Topic history
2. latest offset from Topic
3. timestamp offset from Topic
4. n-offset from Topic history

Channel type:
1. deleteOnExit(must be exclusive)
2. exclusive(only one instance of consumer allowed)
3. shared

Message Disk format
0. offset: 8
1. timestamp: 8
2. msgid: 16
3. ctrl: 1
4. tag: 256 [len+data]
5. length: 4

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

C2B
cmd=produce
cmd=consume
cmd=declare-topic
cmd=declare-channel
cmd=query-topic
cmd=query-channel
cmd=remove-topic
cmd=remove-channel

B2C
cmd=produce-ack
cmd=consume-ack
cmd=response(to declare/query/remove...)
cmd=stream
cmd=quit

Admin use JSON
Common headers
[R]id={id}
[O]appid={appid}
[O]token={token}

cmd=declare-topic
TopicDeclare{
     + topic : String
     + rpc: Boolean
     + properites: Map<string, string>

}

cmd=query-topic
TopicQuery{
     + topic: String
     + appid: String
     + createdTime: Long
}

cmd=remove-topic
TopicRemove{
     + topic: String
     + appid: String
     + createdTime: Long
}

cmd=declare-channel
ChannelDeclare{
     +topic : String
     +channel: String
     +startOffset: Long
     +startTime: Long
     +exclusive: Boolean
     +deleteOnExit: Boolean
     +tag: String
}

cmd=query-channel
ChannelQuery{
     + topic: String
     + channel: String
}

cmd=remove-channel
ChannelRemove{
     + topic: String
     + channel: String
}

===========
cmd=produce
[R]topic={topic}
[O]tag={tag}
[O]ack=[true]|false
[O]{body}

===========
cmd=consume
[R]topic={topic}
[O]channel={channel}
[O]window={window}
[O]ack=[true]|false

[O]{body}

/rpc/?topic=xxx&&method=plus&&args=1,2
/put/?topic=xxx&&body=zzzzzzzzzzzzzzzz
/get/?topic=xxx&&channel=yyyyyy
/declare-topic/?topic=xxx
/query-topic/?topic=xxx
/remove-topic/?topic=xxx

/declare-channel/?topic=xxx
/query-channel/?topic=xxx
/remove-channel/?topic=xxx