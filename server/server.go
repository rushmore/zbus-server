package main

import (
	"bytes"
	"errors"
	"flag"
	"fmt"
	"io"
	"log"
	"net"
	"os"
	"sync/atomic"
	"time"

	"sync"

	"./proto"
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
}

//NewSession create session
func NewSession(netConn *net.Conn, wsConn *websocket.Conn) *Session {
	sess := &Session{}
	sess.ID = uuid()
	sess.ConsumerCtx.Map = make(map[string]interface{})
	sess.Broken = make(chan bool)

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
			s.Broken <- true // Write failed, socket broken?!
		}
		return err
	}
	_, err := s.netConn.Write(buf.Bytes()) //TODO write may return 0 without err
	if err != nil {
		s.Broken <- true // Write failed, socket broken?!
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
}

var upgrader = Upgrader{}

func handleConnection(conn net.Conn, handler SessionHandler) {
	defer conn.Close()
	bufRead := new(bytes.Buffer)
	var wsConn *websocket.Conn
	session := NewSession(&conn, nil)
	handler.Created(session)
outter:
	for {
		data := make([]byte, 1024)
		n, err := conn.Read(data)
		if err != nil {
			handler.OnError(err, session)
			break
		}
		bufRead.Write(data[0:n])

		for {
			req := DecodeMessage(bufRead)
			if req == nil {
				bufRead2 := new(bytes.Buffer)
				bufRead2.Write(bufRead.Bytes())
				bufRead = bufRead2
				break
			}

			//upgrade to Websocket if requested
			if IsWebSocketUpgrade(&req.Header) {
				wsConn, err = upgrader.Upgrade(conn, req)
				if err == nil {
					//log.Printf("Upgraded to websocket: %s\n", req)
					session.Upgrade(wsConn)
					break outter
				}
			}

			go handler.OnMessage(req, session)
		}
	}

	if wsConn != nil { //upgraded to Websocket
		bufRead = new(bytes.Buffer)
		for {
			_, data, err := wsConn.ReadMessage()
			if err != nil {
				handler.OnError(err, session)
				break
			}
			bufRead.Write(data)
			req := DecodeMessage(bufRead)
			if req == nil {
				err = errors.New("Websocket invalid message: " + string(data))
				handler.OnError(err, session)
				break
			}
			if IsWebSocketUpgrade(&req.Header) {
				continue
			}
			go handler.OnMessage(req, session)
		}
	}
	handler.ToDestroy(session)
}

//Server = MqServer + Tracker
type Server struct {
	ServerAddress *proto.ServerAddress
	MqTable       map[string]*MessageQueue
	consumerTable *consumerTable

	MqDir       string
	TrackerList []string

	infoVersion int64
	trackerOnly bool

	tracker *Tracker
}

func newServer() *Server {
	s := &Server{}
	s.consumerTable = newConsumerTable()
	s.infoVersion = time.Now().UnixNano() / int64(time.Millisecond)
	s.trackerOnly = false
	return s
}

func (s *Server) serverInfo() *proto.ServerInfo {
	info := &proto.ServerInfo{}
	info.ServerAddress = s.ServerAddress
	info.ServerVersion = proto.VersionValue
	atomic.AddInt64(&s.infoVersion, 1)
	info.InfoVersion = s.infoVersion
	info.TrackerList = []proto.ServerAddress{}
	info.TopicTable = make(map[string]*proto.TopicInfo)
	for key, mq := range s.MqTable {
		info.TopicTable[key] = mq.TopicInfo()
	}
	for _, address := range s.TrackerList {
		sa := &proto.ServerAddress{Address: address, SslEnabled: false}
		info.TrackerList = append(info.TrackerList, *sa)
	}
	s.addServerContext(info)
	return info
}

func (s *Server) trackerInfo() *proto.TrackerInfo {
	info := &proto.TrackerInfo{}
	info.ServerAddress = s.ServerAddress
	info.ServerVersion = proto.VersionValue
	atomic.AddInt64(&s.infoVersion, 1)
	info.InfoVersion = s.infoVersion
	info.ServerTable = make(map[string]*proto.ServerInfo)
	for key, serverInfo := range s.tracker.serverTable {
		info.ServerTable[key] = serverInfo
	}
	if !s.trackerOnly {
		info.ServerTable[s.ServerAddress.String()] = s.serverInfo()
	}
	return info
}

func (s *Server) addServerContext(t interface{}) {
	switch t.(type) {
	case *proto.TopicInfo:
		info := t.(*proto.TopicInfo)
		info.ServerAddress = s.ServerAddress
		info.ServerVersion = proto.VersionValue
		info.ConsumerCount = int32(s.consumerTable.countForTopic(info.TopicName))
		for _, groupInfo := range info.ConsumeGroupList {
			groupInfo.ConsumerCount = int32(s.consumerTable.countForGroup(info.TopicName, groupInfo.GroupName))
		}
	case *proto.ServerInfo:
		info := t.(*proto.ServerInfo)
		info.ServerAddress = s.ServerAddress
		info.ServerVersion = proto.VersionValue
		for _, topicInfo := range info.TopicTable {
			s.addServerContext(topicInfo)
		}
	case *proto.TrackerInfo:
		info := t.(*proto.TrackerInfo)
		info.ServerAddress = s.ServerAddress
		info.ServerVersion = proto.VersionValue
		for _, serverInfo := range info.ServerTable {
			s.addServerContext(serverInfo)
		}
	}
}

//Options stores the conguration for server
type Options struct {
	Address      string
	MqDir        string
	LogDir       string
	CertFileDir  string
	LogToConsole bool
	Verbose      bool
	TrackOnly    bool
	TrackerList  string
}

//NewOptions creates default configuration
func NewOptions() *Options {
	opt := &Options{}
	opt.Address = "0.0.0.0:15555"
	opt.MqDir = "/tmp/zbus"
	opt.TrackOnly = false
	opt.Verbose = true

	return opt
}

func main() {
	//printBanner()
	log.SetFlags(log.Lshortfile | log.Ldate | log.Ltime)

	opt := NewOptions()
	flag.StringVar(&opt.Address, "addr", "0.0.0.0:15555", "Server address")
	flag.StringVar(&opt.MqDir, "mqdir", "/tmp/zbus", "Message Queue directory")
	flag.StringVar(&opt.LogDir, "logdir", "", "Log file location")
	flag.StringVar(&opt.TrackerList, "tracker", "", "Tracker list")
	flag.BoolVar(&opt.TrackOnly, "trackonly", false, "True--Work as Tracker only, False--MqServer+Tracker")

	flag.Parse()

	var logTargets []io.Writer
	if opt.LogToConsole {
		logTargets = append(logTargets, os.Stdout)
	}
	if opt.LogDir != "" {

	}
	if logTargets != nil {
		w := io.MultiWriter(logTargets...)
		log.SetOutput(w)
	}

	if err := EnsureDir(opt.MqDir); err != nil {
		log.Printf("MqDir(%s) creation failed:%s", opt.MqDir, err.Error())
		return
	}

	tcpAddr, err := net.ResolveTCPAddr("tcp", opt.Address)
	if err != nil {
		log.Println("Error addres:", err.Error())
		return
	}
	fd, err := net.ListenTCP("tcp", tcpAddr)
	if err != nil {
		log.Println("Error listening:", err.Error())
		return
	}
	defer fd.Close()

	log.Println("Listening on " + opt.Address)
	addr := ServerAddress(opt.Address) //get real server address if needs
	server := newServer()
	server.MqDir = opt.MqDir
	server.trackerOnly = opt.TrackOnly
	server.ServerAddress = &proto.ServerAddress{addr, false}
	server.TrackerList = SplitClean(opt.TrackerList, ";")

	mqTable, err := LoadMqTable(server.MqDir)
	if err != nil {
		log.Println("Error loading MQ table: ", err.Error())
		return
	}
	server.MqTable = mqTable

	tracker := NewTracker(server)
	tracker.JoinUpstreams(opt.TrackerList)
	server.tracker = tracker

	handler := NewServerHandler(server)
	for {
		conn, err := fd.AcceptTCP()
		if err != nil {
			log.Println("Error accepting: ", err.Error())
			return
		}
		go handleConnection(conn, handler)
	}
}

func printBanner() {
	fmt.Println(`
                /\\\       
                \/\\\        
                 \/\\\    
     /\\\\\\\\\\\ \/\\\         /\\\    /\\\  /\\\\\\\\\\     
     \///////\\\/  \/\\\\\\\\\  \/\\\   \/\\\ \/\\\//////     
           /\\\/    \/\\\////\\\ \/\\\   \/\\\ \/\\\\\\\\\\    
          /\\\/      \/\\\  \/\\\ \/\\\   \/\\\ \////////\\\  
         /\\\\\\\\\\\ \/\\\\\\\\\  \//\\\\\\\\\   /\\\\\\\\\\  
         \///////////  \/////////    \/////////   \////////// 
		
		`)
}
