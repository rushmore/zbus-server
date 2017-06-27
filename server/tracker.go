package main

import (
	"encoding/json"
	"log"
	"strings"
	"time"

	"./proto"
)

//Tracker tracks MqServers
//Tracker can works as client or server mode, when in server mode, there is no upstream trackers,
//the tracker only accept downstream trackers. When in client mode, the tracker only connects to upstream
//trackers and report data changes in current server(MqServer)
type Tracker struct {
	infoVersion int64

	upstreams        map[string]*MessageClient //Client mode: connect to upstream Trackers
	healthyUpstreams map[string]*MessageClient //Client mode: connected upstream Trackers
	downstreams      map[string]*MessageClient //Server mode: connected downstream MqServer

	subscribers map[string]*Session
	serverTable map[string]*proto.ServerInfo

	reconnectInterval time.Duration
	server            *Server
}

//NewTracker create Tracker
func NewTracker(server *Server) *Tracker {
	t := &Tracker{}
	t.infoVersion = CurrMillis()

	t.upstreams = make(map[string]*MessageClient)
	t.healthyUpstreams = make(map[string]*MessageClient)
	t.downstreams = make(map[string]*MessageClient)
	t.subscribers = make(map[string]*Session)
	t.serverTable = make(map[string]*proto.ServerInfo)
	t.server = server
	t.reconnectInterval = 3000 * time.Millisecond
	return t
}

//JoinUpstreams connects to upstream trackers
func (t *Tracker) JoinUpstreams(trackerList string) {
	trackerList = strings.TrimSpace(trackerList)
	if trackerList == "" {
		return
	}

	addressList := SplitClean(trackerList, ";")
	for _, trackerAddress := range addressList {
		client := t.upstreams[trackerAddress]
		if client != nil {
			continue //already exists
		}
		client = t.connectToServer(trackerAddress)
		client.onConnected = func(c *MessageClient) {
			log.Printf("Connected to Tracker(%s)\n", trackerAddress)
			event := &proto.ServerEvent{}
			event.ServerInfo = t.server.serverInfo()
			event.Live = true

			t.updateToUpstream(c, event)
			t.healthyUpstreams[trackerAddress] = c
		}

		client.onDisconnected = func(c *MessageClient) {
			log.Printf("Disconnected from Tracker(%s)\n", trackerAddress)
			delete(t.healthyUpstreams, trackerAddress)
			time.Sleep(t.reconnectInterval)

			notify := make(chan bool)
			c.EnsureConnected(notify)
			<-notify
		}
		t.upstreams[trackerAddress] = client

		client.EnsureConnected(nil)
	}
}

//Publish server info to subscribers/upstream trackers
func (t *Tracker) Publish() {
	if len(t.healthyUpstreams) > 0 {
		event := &proto.ServerEvent{}
		event.ServerInfo = t.server.serverInfo()
		event.Live = true
		for _, client := range t.healthyUpstreams {
			t.updateToUpstream(client, event)
		}
	}

	if len(t.subscribers) <= 0 {
		return
	}

	//Publish tracker info to all subscribers
	info := t.server.trackerInfo()

	data, _ := json.Marshal(info)
	msg := NewMessage()
	msg.Status = 200
	msg.SetCmd(proto.TrackPub)
	msg.SetJsonBody(string(data))

	var errSessions []*Session
	for _, sess := range t.subscribers {
		err := sess.WriteMessage(msg)
		if err != nil {
			errSessions = append(errSessions, sess)
		}
	}
	for _, sess := range errSessions {
		delete(t.subscribers, sess.ID)
	}
}

//CleanSession remove session from subscribers
func (t *Tracker) CleanSession(sess *Session) {
	delete(t.subscribers, sess.ID)
}

//Close clean the tracker
func (t *Tracker) Close() {
	for _, client := range t.upstreams {
		client.Close()
	}
	t.upstreams = make(map[string]*MessageClient)

	for _, client := range t.downstreams {
		client.Close()
	}
	t.downstreams = make(map[string]*MessageClient)
}

func (t *Tracker) updateToUpstream(upstream *MessageClient, event *proto.ServerEvent) {
	msg := NewMessage()
	msg.SetCmd(proto.TrackPub)
	data, _ := json.Marshal(event)
	msg.SetJsonBody(string(data))
	msg.SetAck(false)

	upstream.Send(msg)
}

func (t *Tracker) connectToServer(trackerAddress string) *MessageClient {
	client := NewMessageClient(trackerAddress, nil) //TODO handle SSL
	return client
}

/////////////////////////////Handlers for Tracker//////////////////////////////////
//trackerHandler serve SrackerInfo request
func trackerHandler(s *ServerHandler, req *Message, sess *Session) {
	if !auth(s, req, sess) {
		return
	}
	info := s.server.trackerInfo()
	data, _ := json.Marshal(info)
	reply(200, req.Id(), string(data), sess)
}

//trackPubHandler server publish of ServerInfo
func trackPubHandler(s *ServerHandler, req *Message, sess *Session) {
	if !auth(s, req, sess) {
		return
	}
	event := &proto.ServerEvent{}
	err := json.Unmarshal(req.body, event)
	if err != nil {
		log.Printf("TrackPub message format error\n")
		return
	}
	serverInfo := event.ServerInfo
	if serverInfo.ServerAddress == s.server.ServerAddress {
		return //no need to hanle data from same server
	}
	tracker := s.tracker
	addressKey := serverInfo.ServerAddress.Address
	client := tracker.downstreams[addressKey]
	if event.Live {
		tracker.serverTable[addressKey] = serverInfo
	} else {
		delete(tracker.serverTable, addressKey)
		if client != nil {
			delete(tracker.downstreams, addressKey)
			client.Close()
		}
	}

	if event.Live && client == nil { //new downstream server joined
		client := tracker.connectToServer(serverInfo.ServerAddress.Address)

		client.onConnected = func(c *MessageClient) {
			log.Printf("Server(%s) in track", serverInfo.ServerAddress.String())
			tracker.downstreams[addressKey] = client
			tracker.Publish()
		}
		client.onDisconnected = func(c *MessageClient) {
			log.Printf("Server(%s) lost of tracking", serverInfo.ServerAddress.String())
			delete(tracker.serverTable, addressKey)
			tracker.Publish()
			client.Close()
		}
		client.Start(nil)
	}

	tracker.Publish() //publish to subscribers
}

//trackSubHandler serve TrackSub request
func trackSubHandler(s *ServerHandler, req *Message, sess *Session) {
	if !auth(s, req, sess) {
		return
	}
	s.tracker.subscribers[sess.ID] = sess

	info := s.server.trackerInfo()
	data, _ := json.Marshal(info)

	resp := NewMessage()
	resp.Status = 200
	resp.SetCmd(proto.TrackPub)
	resp.SetId(req.Id())
	resp.SetJsonBody(string(data))
	sess.WriteMessage(resp)
}
