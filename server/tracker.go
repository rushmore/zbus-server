package main

import (
	"encoding/json"
	"log"
	"path"
	"time"

	"./proto"
)

//Tracker tracks MqServers
//Tracker can works as client or server mode, when in server mode, there is no upstream trackers,
//the tracker only accept downstream trackers. When in client mode, the tracker only connects to upstream
//trackers and report data changes in current server(MqServer)
type Tracker struct {
	infoVersion int64

	upstreams        SyncMap // map[string]*MessageClient, Client mode: connect to upstream Trackers
	healthyUpstreams SyncMap // map[string]*MessageClient, Client mode: connected upstream Trackers
	downstreams      SyncMap // map[string]*MessageClient, Server mode: connected downstream MqServer

	subscribers SyncMap // map[string]*Session
	serverTable SyncMap // map[string]*proto.ServerInfo

	reconnectInterval time.Duration
	server            *Server
}

//NewTracker create Tracker
func NewTracker(server *Server) *Tracker {
	t := &Tracker{}
	t.infoVersion = CurrMillis()

	t.upstreams.Map = make(map[string]interface{})
	t.healthyUpstreams.Map = make(map[string]interface{})
	t.downstreams.Map = make(map[string]interface{})
	t.subscribers.Map = make(map[string]interface{})
	t.serverTable.Map = make(map[string]interface{})
	t.server = server
	t.reconnectInterval = 3000 * time.Millisecond
	return t
}

//JoinUpstreams connects to upstream trackers
func (t *Tracker) JoinUpstreams(trackerList []*proto.ServerAddress) {
	for _, trackerAddress := range trackerList {
		key := trackerAddress.String()
		client, _ := t.upstreams.Get(key).(*MqClient)
		if client != nil {
			continue //already exists
		}

		client = t.connectToServer(trackerAddress)

		client.onConnected = func(c *MqClient) {
			info, err := client.QueryServer()
			if err != nil {
				trackerAddress = info.ServerAddress
			}
			log.Printf("Connected to Tracker(%s)\n", trackerAddress.String())

			event := &proto.ServerEvent{}
			event.ServerInfo = t.server.serverInfo()
			event.Live = true

			t.updateToUpstream(c, event)
			t.healthyUpstreams.Set(trackerAddress.String(), c)
			t.upstreams.Set(trackerAddress.String(), client)
		}

		client.onDisconnected = func(c *MqClient) {
			log.Printf("Disconnected from Tracker(%s)\n", trackerAddress.String())
			t.healthyUpstreams.Remove(trackerAddress.String())
			time.Sleep(t.reconnectInterval)

			notify := make(chan bool)
			c.EnsureConnected(notify)
			<-notify
		}
		client.Start(nil)
	}
}

//PubToAll publish ServerInfo to both Trackers and Subscribers
func (t *Tracker) PubToAll() {
	if t.healthyUpstreams.Size() > 0 {
		event := &proto.ServerEvent{}
		event.ServerInfo = t.server.serverInfo()
		event.Live = true

		t.healthyUpstreams.RLock()
		for _, c := range t.healthyUpstreams.Map {
			client, _ := c.(*MqClient)
			t.updateToUpstream(client, event)
		}
		t.healthyUpstreams.RUnlock()
	}

	t.PubToSubscribers()
}

//PubToSubscribers only publish ServerInfo to subscriber clients
func (t *Tracker) PubToSubscribers() {
	if t.subscribers.Size() <= 0 {
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
	t.subscribers.RLock()
	for _, s := range t.subscribers.Map {
		sess, _ := s.(*Session)
		err := sess.WriteMessage(msg)
		if err != nil {
			errSessions = append(errSessions, sess)
		}
	}
	t.subscribers.RUnlock()
	for _, sess := range errSessions {
		t.subscribers.Remove(sess.ID)
	}
}

//CleanSession remove session from subscribers
func (t *Tracker) CleanSession(sess *Session) {
	t.subscribers.Remove(sess.ID)
}

//Close clean the tracker
func (t *Tracker) Close() {
	t.upstreams.RLock()
	for _, c := range t.upstreams.Map {
		client, _ := c.(*MqClient)
		client.Close()
	}
	t.upstreams.RUnlock()
	t.upstreams.Clear()

	t.downstreams.RLock()
	for _, c := range t.downstreams.Map {
		client, _ := c.(*MqClient)
		client.Close()
	}
	t.downstreams.RUnlock()
	t.downstreams.Clear()
}

func (t *Tracker) updateToUpstream(upstream *MqClient, event *proto.ServerEvent) {
	msg := NewMessage()
	msg.SetCmd(proto.TrackPub)
	data, _ := json.Marshal(event)
	msg.SetJsonBody(string(data))
	msg.SetAck(false)

	upstream.Send(msg)
}

func (t *Tracker) connectToServer(trackerAddress *proto.ServerAddress) *MqClient {
	var certFile *string
	if trackerAddress.SslEnabled {
		config := t.server.Config
		if file, ok := config.CertFileTable[trackerAddress.Address]; ok {
			fileFullPath := path.Join(config.CertFileDir, file)
			certFile = &fileFullPath
		} else {
			log.Printf("Missing certificate file to TLS/SSL connecting to (%s)", trackerAddress.Address)
			return nil
		}
	}
	client := NewMqClient(trackerAddress.Address, certFile)
	return client
}

/////////////////////////////Handlers for Tracker//////////////////////////////////
//trackerHandler serve SrackerInfo request
func trackerHandler(s *Server, req *Message, sess *Session) {
	if !auth(s, req, sess) {
		return
	}
	info := s.trackerInfo()
	data, _ := json.Marshal(info)
	reply(200, req.Id(), string(data), sess)
}

//trackPubHandler server publish of ServerInfo
func trackPubHandler(s *Server, req *Message, sess *Session) {
	if !auth(s, req, sess) {
		return
	}
	event := &proto.ServerEvent{}
	err := json.Unmarshal(req.body, event)
	if err != nil {
		log.Printf("TrackPub message format error\n")
		return
	}
	pubServer := event.ServerInfo
	if pubServer.ServerAddress == s.ServerAddress {
		return //no need to hanle data from same server
	}
	tracker := s.tracker
	addressKey := pubServer.ServerAddress.Address
	client, _ := tracker.downstreams.Get(addressKey).(*MqClient)
	if event.Live {
		tracker.serverTable.Set(addressKey, pubServer)
	} else {
		tracker.serverTable.Remove(addressKey)
		if client != nil {
			tracker.downstreams.Remove(addressKey)
			client.Close()
		}
	}

	if event.Live && client == nil { //new downstream server joined
		client := tracker.connectToServer(pubServer.ServerAddress)

		client.onConnected = func(c *MqClient) {
			log.Printf("Server(%s) in track", pubServer.ServerAddress.String())
			tracker.downstreams.Set(addressKey, client)
			tracker.PubToSubscribers()
		}
		client.onDisconnected = func(c *MqClient) {
			log.Printf("Server(%s) lost of tracking", pubServer.ServerAddress.String())
			tracker.serverTable.Remove(addressKey)
			tracker.downstreams.Remove(addressKey)
			tracker.PubToSubscribers()
			client.Close()
		}
		client.Start(nil)
	}

	tracker.PubToSubscribers()
}

//trackSubHandler serve TrackSub request
func trackSubHandler(s *Server, req *Message, sess *Session) {
	if !auth(s, req, sess) {
		return
	}
	s.tracker.subscribers.Set(sess.ID, sess)

	info := s.trackerInfo()
	data, _ := json.Marshal(info)

	resp := NewMessage()
	resp.Status = 200
	resp.SetCmd(proto.TrackPub)
	resp.SetId(req.Id())
	resp.SetJsonBody(string(data))
	err := sess.WriteMessage(resp)
	if err != nil {
		log.Printf("TrackSub write error: %s", err.Error())
	}
}
