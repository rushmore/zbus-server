package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"log"
	"strings"

	"./proto"
)

const (
	lastConsumeMsgIDKey = "msgid"
)

var restCommands = []string{
	proto.Produce,
	proto.Consume,
	proto.Declare,
	proto.Query,
	proto.Remove,
	proto.Empty,
}

func isRestCommand(cmd string) bool {
	for _, value := range restCommands {
		if value == cmd {
			return true
		}
	}
	return false
}

//ServerHandler manages session from clients
type ServerHandler struct {
	SessionTable  SyncMap                                             //Safe
	handlerTable  map[string]func(*ServerHandler, *Message, *Session) //readonly
	consumerTable *consumerTable

	serverAddress string
	server        *Server
	tracker       *Tracker
}

//NewServerHandler create ServerSessionHandler
func NewServerHandler(server *Server) *ServerHandler {
	s := &ServerHandler{}
	s.SessionTable.Map = make(map[string]interface{})
	s.consumerTable = server.consumerTable
	s.handlerTable = make(map[string]func(*ServerHandler, *Message, *Session))
	s.serverAddress = server.ServerAddress.Address
	s.server = server
	s.tracker = server.tracker

	s.handlerTable["favicon.ico"] = faviconHandler
	s.handlerTable[proto.Heartbeat] = heartbeatHandler

	s.handlerTable[proto.Home] = homeHandler
	s.handlerTable[proto.Js] = jsHandler
	s.handlerTable[proto.Css] = cssHandler
	s.handlerTable[proto.Img] = imgHandler
	s.handlerTable[proto.Page] = pageHandler
	s.handlerTable[proto.Produce] = produceHandler
	s.handlerTable[proto.Consume] = consumeHandler
	s.handlerTable[proto.Rpc] = rpcHandler
	s.handlerTable[proto.Route] = routeHandler
	s.handlerTable[proto.Declare] = declareHandler
	s.handlerTable[proto.Query] = queryHandler
	s.handlerTable[proto.Remove] = removeHandler
	s.handlerTable[proto.Empty] = emptyHandler
	s.handlerTable[proto.Tracker] = trackerHandler
	s.handlerTable[proto.Server] = serverHandler
	s.handlerTable[proto.TrackPub] = trackPubHandler
	s.handlerTable[proto.TrackSub] = trackSubHandler
	return s
}

//Created when new session from client joined
func (s *ServerHandler) Created(sess *Session) {
	log.Printf("Session(%s) Created", sess)
	s.SessionTable.Set(sess.ID, sess)
}

//ToDestroy when connection from client going to close
func (s *ServerHandler) ToDestroy(sess *Session) {
	log.Printf("Session(%s) Destroyed", sess)
	s.cleanSession(sess)
}

//OnError when socket error occured
func (s *ServerHandler) OnError(err error, sess *Session) {
	log.Printf("Session(%s) Error: %s", sess, err)
	select {
	case sess.Broken <- true:
	default: //ignore
	}
	s.cleanSession(sess)
}

func (s *ServerHandler) cleanSession(sess *Session) {
	s.tracker.CleanSession(sess)
	s.SessionTable.Remove(sess.ID)

	isConsumer := false

	sess.ConsumerCtx.RLock()
	for topic, g := range sess.ConsumerCtx.Map {
		groups, _ := g.(*SyncMap)
		if groups == nil {
			continue
		}
		for group := range groups.Map {
			s.consumerTable.removeSession(sess, topic, group)
			isConsumer = true
		}
	}
	sess.ConsumerCtx.RUnlock()

	if isConsumer {
		s.tracker.Publish()
	}
}

func (s *ServerHandler) cleanMq(topic string) {
	s.consumerTable.removeSession(nil, topic, "")
}

//OnMessage when message available on socket
func (s *ServerHandler) OnMessage(msg *Message, sess *Session) {
	msg.SetHeader(proto.Sender, sess.ID)
	msg.SetHeader(proto.Host, s.serverAddress)
	if msg.Id() == "" {
		msg.SetId(uuid())
	}
	if msg.Cmd() != proto.Heartbeat {
		//log.Printf(msg.String())
	}

	handleUrlMessage(msg)

	cmd := msg.Cmd()
	handler, ok := s.handlerTable[cmd]
	if ok {
		handler(s, msg, sess)
		return
	}
	res := NewMessageStatus(400, "Bad format: command(%s) not support", cmd)
	sess.WriteMessage(res)
}

func handleUrlMessage(msg *Message) {
	if msg.GetHeaderNil(proto.Cmd) != nil {
		return
	}
	url := msg.Url
	if url == "/" {
		return
	}
	idx := strings.IndexByte(url, '?')
	var cmd string
	kvstr := ""
	if idx >= 0 {
		cmd = url[1:idx]
		kvstr = url[idx+1:]
	} else {
		cmd = url[1:]
	}

	topicStart := strings.IndexByte(cmd, '/')
	if topicStart > 0 {
		rest := cmd[topicStart+1:]

		cmd = cmd[0:topicStart]
		if cmd == proto.Rpc {
			handleUrlRpc(msg, rest, kvstr)
		} else if isRestCommand(cmd) {
			bb := SplitClean(rest, "/")
			if len(bb) > 0 {
				msg.SetHeaderIfNone(proto.Topic, bb[0])
			}
			if len(bb) > 1 {
				msg.SetHeaderIfNone(proto.ConsumeGroup, bb[1])
			}
		}
	}
	msg.SetCmd(strings.ToLower(cmd))

	if cmd != proto.Rpc && kvstr != "" {
		handleUrlKVs(msg, kvstr)
	}
}

func handleUrlKVs(msg *Message, kvstr string) {
	if kvstr == "" {
		return
	}
	kvs := SplitClean(kvstr, "&")
	for _, kv := range kvs {
		bb := SplitClean(kv, "=")
		if len(bb) != 2 {
			continue
		}
		key, val := bb[0], bb[1]
		if strings.EqualFold(key, "body") && msg.body == nil {
			msg.SetBodyString(string(val))
		}
		msg.SetHeaderIfNone(key, val)
	}
}

type request struct {
	Method string   `json:"method,omitempty"`
	Params []string `json:"params,omitempty"`
	Module string   `json:"module,omitempty"`
}

func handleUrlRpc(msg *Message, rest string, kvstr string) {
	// <topic>/<method>/<param_1>/../<param_n>[?module=<module>&&<header_ext_kvs>]
	handleUrlKVs(msg, kvstr)
	bb := SplitClean(rest, "/")
	if len(bb) < 2 {
		return //invalid
	}
	msg.SetHeaderIfNone(proto.Topic, bb[0])
	method := bb[1]
	var params []string
	for i := 2; i < len(bb); i++ {
		params = append(params, bb[i])
	}
	req := &request{method, params, msg.GetHeader("module")}
	data, _ := json.Marshal(req)
	msg.SetBody(data)
}

func auth(s *ServerHandler, msg *Message, sess *Session) bool {
	return true
}

func findMQ(s *ServerHandler, req *Message, sess *Session) *MessageQueue {
	topic := req.Topic()
	if topic == "" {
		reply(400, req.Id(), "Missing topic", sess)
		return nil
	}
	mq := s.server.MqTable[topic]
	if mq == nil {
		body := fmt.Sprintf("Topic(%s) not found", topic)
		reply(404, req.Id(), body, sess)
		return nil
	}
	return mq
}

func produceHandler(s *ServerHandler, req *Message, sess *Session) {
	if !auth(s, req, sess) {
		return
	}
	mq := findMQ(s, req, sess)
	if mq == nil {
		return
	}

	mq.Write(req)

	if req.Ack() {
		body := fmt.Sprintf("%d", CurrMillis())
		reply(200, req.Id(), body, sess)
	}
}

func consumeHandler(s *ServerHandler, req *Message, sess *Session) {
	if !auth(s, req, sess) {
		return
	}
	mq := findMQ(s, req, sess)
	if mq == nil {
		return
	}
	topic := mq.Name()
	group := req.ConsumeGroup()
	if group == "" {
		group = topic
	}

	newConsumer := false
	if sess.getConsumerCtx(topic, group) == nil {
		s.consumerTable.addSession(sess, topic, group)
		newConsumer = true
	}

	sess.setConsumerCtx(topic, group, req.Id())

	if newConsumer {
		go s.tracker.Publish()
	}

	reader := mq.ConsumeGroup(group)
	if reader == nil {
		reply(404, req.Id(), fmt.Sprintf("ConsumeGroup(%s) not found", group), sess)
		return
	}

consumeRead:
	for {
		data, err := reader.Read()
		if err != nil {
			reply(500, req.Id(), fmt.Sprintf("Consume read error: %s", err.Error()), sess)
			return
		}

		if data == nil {
			select { //TODO timeout!
			case <-sess.Broken:
				break consumeRead
			case <-reader.Available:
			}
			continue
		}

		buf := bytes.NewBuffer(data.Body)
		resp := DecodeMessage(buf)

		resp.SetOriginId(resp.Id())
		msgid, _ := sess.getConsumerCtx(topic, group).(string)
		resp.SetId(msgid)
		if resp.Status == 0 {
			resp.Status = 200
			resp.SetOriginUrl(resp.Url)
		}
		sess.WriteMessage(resp)
		break
	}
}

func rpcHandler(s *ServerHandler, req *Message, sess *Session) {
	req.SetAck(false)
	produceHandler(s, req, sess)
}

func routeHandler(s *ServerHandler, req *Message, sess *Session) {
	recver := req.Recver()
	if recver == "" {
		log.Printf("Warn: missing recver")
		return //ignore
	}
	target, _ := s.SessionTable.Get(recver).(*Session)
	if target == nil {
		log.Printf("Warn: missing target(%s)", recver)
		return //ignore
	}

	req.RemoveHeader(proto.Ack)
	req.RemoveHeader(proto.Recver)
	req.RemoveHeader(proto.Cmd)

	req.Status = 200
	originStatus := req.OriginStatus()
	if originStatus != nil {
		req.Status = *originStatus
		req.RemoveHeader(proto.OriginStatus)
	}

	target.WriteMessage(req)
}

func declareHandler(s *ServerHandler, req *Message, sess *Session) {
	if !auth(s, req, sess) {
		return
	}
	topic := req.Topic()
	if topic == "" {
		reply(400, req.Id(), "Missing topic", sess)
		return
	}
	g := &ConsumeGroup{}
	g.LoadFrom(req)
	declareGroup := g.GroupName != ""
	if g.GroupName == "" {
		g.GroupName = topic
	}

	var err error
	var info interface{}

	mq := s.server.MqTable[topic]
	if mq == nil {
		mq, err = NewMessageQueue(s.server.MqDir, topic)
		if err != nil {
			body := fmt.Sprintf("Delcare Topic error: %s", err.Error())
			reply(500, req.Id(), body, sess)
			return
		}
		mq.SetCreator(req.Token()) //token as creator, TODO
		mask := req.TopicMask()
		if mask != nil {
			mq.SetMask(*mask)
		}
		s.server.MqTable[topic] = mq

		info, err = mq.DeclareGroup(g)
		if err != nil {
			body := fmt.Sprintf("Delcare ConsumeGroup error: %s", err.Error())
			reply(500, req.Id(), body, sess)
			return
		}
	} else {
		mask := req.TopicMask()
		if mask != nil {
			mq.SetMask(*mask)
		}

		if declareGroup {
			info, err = mq.DeclareGroup(g)
			if err != nil {
				body := fmt.Sprintf("Delcare ConsumeGroup error: %s", err.Error())
				reply(500, req.Id(), body, sess)
				return
			}
		}
	}

	if !declareGroup {
		info = mq.TopicInfo()
	}

	s.server.addServerContext(info) //require server info attach
	data, _ := json.Marshal(info)
	replyJson(200, req.Id(), string(data), sess)

	s.tracker.Publish()
}

func queryHandler(s *ServerHandler, req *Message, sess *Session) {
	if !auth(s, req, sess) {
		return
	}
	mq := findMQ(s, req, sess)
	if mq == nil {
		return
	}

	var info interface{}
	group := req.ConsumeGroup()
	if group == "" {
		info = mq.TopicInfo()
	} else {
		groupInfo := mq.GroupInfo(group)
		if groupInfo == nil {
			body := fmt.Sprintf("ConsumeGroup(%s) not found", group)
			reply(404, req.Id(), body, sess)
			return
		}
		info = groupInfo
	}

	s.server.addServerContext(info)
	data, _ := json.Marshal(info)
	replyJson(200, req.Id(), string(data), sess)
}

func removeHandler(s *ServerHandler, req *Message, sess *Session) {
	if !auth(s, req, sess) {
		return
	}
	mq := findMQ(s, req, sess)
	if mq == nil {
		return
	}
	topic := mq.Name()
	group := req.ConsumeGroup()
	if group == "" {
		delete(s.server.MqTable, mq.Name())
		err := mq.Destroy()
		if err != nil {
			body := fmt.Sprintf("Remove topic(%s) error: %s", topic, err.Error())
			reply(500, req.Id(), body, sess)
			return
		}
	} else {
		if mq.ConsumeGroup(group) == nil {
			body := fmt.Sprintf("ConsumeGroup(%s) not found", group)
			reply(404, req.Id(), body, sess)
			return
		}

		err := mq.RemoveGroup(group)
		if err != nil {
			body := fmt.Sprintf("Remove ConsumeGroup(%s) error: %s", group, err.Error())
			reply(500, req.Id(), body, sess)
			return
		}
	}

	reply(200, req.Id(), fmt.Sprintf("%d", CurrMillis()), sess)

	s.tracker.Publish()
}

func emptyHandler(s *ServerHandler, req *Message, sess *Session) {
	if !auth(s, req, sess) {
		return
	}
	reply(500, req.Id(), "Not Implemented", sess)
}

func serverHandler(s *ServerHandler, req *Message, sess *Session) {
	if !auth(s, req, sess) {
		return
	}

	res := NewMessage()
	res.Status = 200
	info := s.server.serverInfo()
	data, _ := json.Marshal(info)
	replyJson(200, req.Id(), string(data), sess)
}

func reply(status int, msgid string, body string, sess *Session) {
	resp := NewMessageStatus(status, body)
	resp.SetId(msgid)
	sess.WriteMessage(resp)
}

func replyJson(status int, msgid string, body string, sess *Session) {
	resp := NewMessageStatus(status)
	resp.SetId(msgid)
	resp.SetJsonBody(body)
	sess.WriteMessage(resp)
}

func renderFile(file string, contentType string, s *ServerHandler, msg *Message, sess *Session) {
	res := NewMessage()
	if file == "" {
		url := msg.Url
		bb := SplitClean(url, "/")
		if len(bb) > 1 {
			file = bb[1]
		}
	}

	data, err := ReadAssetFile(file)
	if err != nil {
		res.Status = 404
		res.SetBodyString("File(%s) error: %s", file, err)
	} else {
		res.Status = 200
		res.SetBody(data)
	}
	res.SetHeader("content-type", contentType)
	sess.WriteMessage(res)
}

func homeHandler(s *ServerHandler, msg *Message, sess *Session) {
	renderFile("home.htm", "text/html", s, msg, sess)
}

func faviconHandler(s *ServerHandler, msg *Message, sess *Session) {
	renderFile("logo.svg", "image/svg+xml", s, msg, sess)
}

func jsHandler(s *ServerHandler, msg *Message, sess *Session) {
	renderFile("", "application/javascript", s, msg, sess)
}

func cssHandler(s *ServerHandler, msg *Message, sess *Session) {
	renderFile("", "text/css", s, msg, sess)
}

func imgHandler(s *ServerHandler, msg *Message, sess *Session) {
	renderFile("", "image/svg+xml", s, msg, sess)
}

func pageHandler(s *ServerHandler, msg *Message, sess *Session) {
	renderFile("", "text/html", s, msg, sess)
}

func heartbeatHandler(s *ServerHandler, msg *Message, sess *Session) {
	//just ignore
}

//topic=> { group => {SessId => Session} }
type consumerTable struct {
	SyncMap
}

func newConsumerTable() *consumerTable {
	c := &consumerTable{}
	c.Map = make(map[string]interface{})
	return c
}

func (t *consumerTable) addSession(sess *Session, topic string, group string) bool {
	t.Lock()
	groups, _ := t.Map[topic].(*SyncMap)
	if groups == nil {
		groups = &SyncMap{Map: make(map[string]interface{})}
		t.Map[topic] = groups
	}
	t.Unlock()

	groups.Lock()
	groupSessTable, _ := groups.Map[group].(*SyncMap)
	if groupSessTable == nil {
		groupSessTable = &SyncMap{Map: make(map[string]interface{})}
		groups.Map[group] = groupSessTable
	}
	groups.Unlock()
	newSession := groupSessTable.Get(sess.ID) == nil
	groupSessTable.Set(sess.ID, sess)
	return newSession
}

func (t *consumerTable) removeSession(sess *Session, topic string, group string) {
	groups := t.Get(topic).(*SyncMap)
	if groups == nil {
		return //ignore
	}
	if group == "" {
		t.Remove(topic)
		return
	}
	groupSessTable := groups.Get(group).(*SyncMap)
	if groupSessTable == nil {
		return
	}
	if sess != nil {
		groupSessTable.Remove(sess.ID)
	}
}

func (t *consumerTable) countForTopic(topic string) int {
	groups, _ := t.Get(topic).(*SyncMap)
	if groups == nil {
		return 0
	}
	groups.RLock()
	n := 0
	for _, g := range groups.Map {
		groupSessTable, _ := g.(*SyncMap)
		if groupSessTable == nil {
			continue
		}
		n += len(groupSessTable.Map)
	}
	groups.RUnlock()
	return n
}

func (t *consumerTable) countForGroup(topic string, group string) int {
	groups, _ := t.Get(topic).(*SyncMap)
	if groups == nil {
		return 0
	}
	groupSessTable, _ := groups.Get(group).(*SyncMap)
	if groupSessTable == nil {
		return 0
	}
	return len(groupSessTable.Map)
}
