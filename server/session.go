package main

import (
	"bytes"
	"fmt"
	"log"
	"net"
	"sync"

	"./websocket"
)

// Session abstract socket connection
type Session struct {
	ID          string
	ConsumerCtx SyncMap
	Broken      chan bool

	netConn     net.Conn
	wsConn      *websocket.Conn
	isWebsocket bool
	wsMutex     sync.Mutex
	active      bool
}

//NewSession create session
func NewSession(netConn *net.Conn, wsConn *websocket.Conn) *Session {
	sess := &Session{}
	sess.ID = uuid()
	sess.ConsumerCtx.Map = make(map[string]interface{})
	sess.Broken = make(chan bool)
	sess.active = true

	if netConn != nil {
		sess.isWebsocket = false
		sess.netConn = *netConn
	}
	if wsConn != nil {
		sess.isWebsocket = true
		sess.wsConn = wsConn
	}
	return sess
}

//Close Change status to inactive, send channel message
func (s *Session) Close() {
	s.active = false
	select {
	case s.Broken <- true:
	default:
		//ignore is alredy broken
	}
}

//Upgrade session to be based on websocket
func (s *Session) Upgrade(wsConn *websocket.Conn) {
	s.wsConn = wsConn
	s.isWebsocket = true
}

//String get string value of session
func (s *Session) String() string {
	return fmt.Sprintf("%s-%s", s.ID, s.netConn.RemoteAddr())
}

//WriteMessage write message to underlying connection
func (s *Session) WriteMessage(msg *Message) error {
	buf := new(bytes.Buffer)
	msg.EncodeMessage(buf)
	if s.isWebsocket {
		s.wsMutex.Lock()
		defer s.wsMutex.Unlock()
		err := s.wsConn.WriteMessage(websocket.BinaryMessage, buf.Bytes())
		if err != nil {
			log.Printf("Write error(%s): %s", s.ID, err.Error())
			s.Close() //send signal
		}
		return err
	}
	_, err := s.netConn.Write(buf.Bytes()) //TODO write may return 0 without err
	if err != nil {
		log.Printf("Write error(%s): %s", s.ID, err.Error())
		s.Close() //send signal
	}
	return err
}

func (s *Session) setConsumerCtx(topic string, group string, ctx interface{}) {
	s.ConsumerCtx.Lock()
	defer s.ConsumerCtx.Unlock()
	groups, _ := s.ConsumerCtx.Map[topic].(*SyncMap)
	if groups == nil {
		groups = &SyncMap{Map: make(map[string]interface{})}
		s.ConsumerCtx.Map[topic] = groups
	}
	groups.Map[group] = ctx
}

func (s *Session) getConsumerCtx(topic string, group string) interface{} {
	groups, _ := s.ConsumerCtx.Get(topic).(*SyncMap)
	if groups == nil {
		return nil
	}
	return groups.Get(group)
}

//SessionHandler handles session lifecyle
type SessionHandler interface {
	Created(sess *Session)
	ToDestroy(sess *Session)
	OnMessage(msg *Message, sess *Session)
	OnError(err error, sess *Session)
	OnIdle(sess *Session)
}
