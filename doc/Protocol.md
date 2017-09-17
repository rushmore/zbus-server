## Protocol

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
	
	/query/topic/[group] 
	
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

