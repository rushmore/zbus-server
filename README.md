# Gist of zbus project (zbus=mq+rpc)
## Do NOT use in production, still alpha

##Foundation
MqClient[Producer+Consumer] <==> MqServer

##Broker
Abstracted connection(s) to MqServer(s)

- Single Server Node
- Multiple Server Nodes
- Dynamic Server Nodes (High Availability)

Features more than just a MqClient

- Pooled for connections
- Plugable client selection algorithm

##Unified API
	
	Producer ==>MqBroker ==> Consumer
	            SingleBroker
	            MultiBroker
	            [ZbusBroker]

##MQ Model
	Topic   |||||||||||||||||||||| <-- Produce Write
	                   ^-------ConsumeGroup1              (reader group1)
	                       ^-------ConsumeGroup2          (reader group2)
	               ^-------ConsumeGroup3                  (reader group3)

Unified model for load balancing(unicast) and pub-sub(multicast)

##RPC based on MQ
- RPC transmission-neutraled model
- MQ-based RPC, centralized + distributed

##HTTP-alike Protocol
Message => HTTP format
 

##Security
AppId + Token

##Monitor Tools
- Connections
- Topics:  Create/Update/Pause/Resume
- ConsumeGroups:  Create/Update/Pause/Resume
- Trafic log
- Trace Message

##ZBUS as Service Bus
- Java/.NET/C_C++/JS/Python api support
- Micro-Service oriented
- zbus-msmq
- zbus-kcxp
- zbus-webservice

##Roadmap
Data Replication
