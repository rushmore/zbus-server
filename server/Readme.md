# Code Dissect
zbus is a TCP server favors HTTP-alike protocol, but **NO** HTTP standard module in Golang involved.

## Networking Server

## Message Protocol

## Topic-ConsumeGroup Disk Queue

## High Aavailability 
        

                       +-----------------+                   |                    +-----------------+
                       |  Client(Broker) |                   |                    |  Client(Broker) |
                       +-----------------+                   |                    +-----------------+ 
                             |     |                         |                          |     |
                  track_sub  |     |   track_sub             |               track_sub  |     |   track_sub
                +------------+     +-------------+           |             +------------+     +-------------+
                |                                |           |             |                                |
                v                                v           |             v                                v
         +-------------+                 +--------------+    |      +-------------+                 +--------------+
         |  Tracker1   |                 |   Tracker2   |   ==>    |     zbus1   | <--track_pub--> |      zbus2   |
         +-------------+                 +--------------+    |      +-------------+                 +--------------+
                ^                                ^           |
                |                                |           |
                +--------------------------------+           |                 Tracker + MqServer =>  zbus
                |  track_pub          track_pub  |           |
           +--------------+             +--------------+     |
           |   MqServer1  |             |   MqServer2  |     |
           +--------------+             +--------------+     |

### Tracker in Server
zbus generates two types of info for topology of Topic/ConsumeGroup
- ServerInfo { TopicName => TopicInfo }, as the role of MqServer.
- TrackerInfo { ServerAddress => ServerInfo }, as the role of Tracker.

A zbus instance can play the role MqServer to handle request from MqClient, or the role of Tracker, to dynamically aggregate MqServers in track. Moreover, a zbus instance is able to play the roles of both MqServer and Tracker together, and by default both roles are enabled, which make HA setup much easier. 

*Commands in Tracker*:

- cmd=tracker, Clients of any kind may obtain TrackerInfo { ServerAddress => ServerInfo }
- cmd=track_pub, Tracked servers publish ServerInfo {TopicName => TopicInfo} to the Tracker
- cmd=track_sub, Clients subscribe to Tracker, get notifications of TrackerInfo { ServerAddress => ServerInfo }

Algorithm notes for Tracker: 

- There is no subscriptions(cmd=track_sub) among servers, only clients subscribe to zbus instance.
- Tracked server only publish its own ServerInfo to Tracker other than TrackerInfo.
- ServerInfo published from Tracked server, is only published to Subscribers, should not cascade to Trackers if available. 
  1) To break ServerInfo message dead looping, if two zbus instances are tracking to each other. 
  2) To ensure the authority of ServerInfo, only generated from the server it comes from.
- Changes of Topic and ConsumeGroup, such as decalre/remove, triggers ServerInfo(**NOT** TrackerInfo) publishing to both Subscribers and Trackers

 
### Tracker in Client


Client may get notifications of TrackerInfo { ServerAddress => ServerInfo } from multiple Tracker instances. TrackerInfo and ServerInfo are both versioned via a property named *InfoVersion*, which is incremental all the time even after the server instances restarted.

*Version Contrlol* 

To prevent *Split Brain* of same ServerInfo, Client's *BrokerRouteTable* only accepts the updates of TrackerInfo with higher *InfoVersion* number, and it is the same case for ServerInfo inside of TrackerInfo to get merged.

*Algorithm - build BrokerRouteTable*

    BrokerRouteTable {
      ServerTable: { ServerAddress => ServerInfo } 
      TopicTable: { TopicName => { ServerAddress => TopicInfo } }
      VotesTable: { TrackerAddress => { Servers: Set[ServerAddress], InfoVersion: <Version> } }
      VoteFactor: 0.5  //Configurable, represents a ServerInfo's votes ratio, [#trackers voted / #trackers in total]
    }

A client like *Broker* (client point view) may subscribe to multiple Trackers, which publish TrackerInfo. Trackers may come and go, or get updated from tracked servers, all of which subsequently trigger the events of updateTracker and removeTracker.

- **updateTracker** (trackerInfo) 
  
      1) Update VotesTable

        case: trackerInfo new, add to VotesTable 
        case: trackerInfo exists, update if InfoVersion is higher

      2) Merge ServerTable

        for each serverInfo in trackerInfo { 
          update to ServerTable only if serverInfo's InfoVersion is higher than existing one 
        }

      3) Purge ServerTable/TopicTable

        for each serverInfo in ServerTable { 
          count #votedTracker via VotesTable 
          remove serverInfo if #votedTracker/#totalTracker < VoteFactor 
        }

      4) Build new TopicTable based on purged ServerTable 

      return removed serverInfos


- **removeTracker** (trackerAddress)

      1) Remove tracker in VotesTable 

      2) Purge ServerTable/TopicTable 



## Goroutine-safe map, SyncMap

## Go Client

## Websocket adaptor
