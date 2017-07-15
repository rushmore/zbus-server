package main

import (
	"bytes"
	"crypto/tls"
	"errors"
	"flag"
	"fmt"
	"io"
	"io/ioutil"
	"log"
	"net"
	"os"
	"sync/atomic"
	"time"

	"encoding/json"

	"./proto"
	"./websocket"
)

//Config stores the conguration for server
type Config struct {
	ServerAddress string        `json:"serverAddress,omitempty"`
	ServerName    string        `json:"serverName,omitempty"` //override Address if provided
	MqDir         string        `json:"mqDir,omitempty"`
	LogDir        string        `json:"logDir,omitempty"`
	LogToConsole  bool          `json:"logToConsole,omitempty"`
	Verbose       bool          `json:"verbose,omitempty"`
	TrackerOnly   bool          `json:"trackerOnly,omitempty"`
	TrackerList   string        `json:"trackerList,omitempty"`
	IdleTimeout   time.Duration `json:"idleTimeout,omitempty"`
	SslEnabled    bool          `json:"sslEnabled,omitempty"`

	ServerCertFile  string            `json:"serverCertFile,omitempty"`
	ServerCertKey   string            `json:"serverCertKey,omitempty"`
	CertFileDir     string            `json:"certFileDir,omitempty"`
	DefaultCertFile string            `json:"defaultCertFile,omitempty"`
	CertFileTable   map[string]string `json:"certFileTable,omitempty"` //readonly after init
}

//Server = MqServer + Tracker
type Server struct {
	Config        *Config
	ServerAddress *proto.ServerAddress
	TrackerList   []*proto.ServerAddress
	MqTable       SyncMap // map[string]*MessageQueue
	SessionTable  SyncMap //Safe

	handlerTable  map[string]func(*Server, *Message, *Session) //readonly
	consumerTable *ConsumerTable

	listener net.Listener

	infoVersion int64

	tracker *Tracker

	wsUpgrader *Upgrader // upgrade TCP to websocket
}

//NewServer create a zbus server
func NewServer(config *Config) *Server {
	s := &Server{}
	s.Config = config
	s.SessionTable.Map = make(map[string]interface{})
	s.handlerTable = make(map[string]func(*Server, *Message, *Session))

	host, port := ServerAddress(config.ServerAddress) //get real server address if needs
	if config.ServerName != "" {
		host = config.ServerName
	}
	addr := fmt.Sprintf("%s:%d", host, port)
	s.ServerAddress = &proto.ServerAddress{addr, config.SslEnabled}
	s.TrackerList = ParseServerAddressList(config.TrackerList)

	s.MqTable.Map = make(map[string]interface{})
	s.consumerTable = newConsumerTable()
	s.infoVersion = CurrMillis()
	s.wsUpgrader = &Upgrader{}

	//init at last
	s.tracker = NewTracker(s)
	s.initServerHandler()

	return s
}

//Start zbus server(MqServer + Tracker)
func (s *Server) Start() error {
	var err error
	if s.listener != nil {
		log.Printf("No need to start again")
		return nil
	}
	if s.Config.SslEnabled {
		cert, err := tls.LoadX509KeyPair(s.Config.ServerCertFile, s.Config.ServerCertKey)
		if err != nil {
			log.Println("Error loading certificate file and key:", err.Error())
			return err
		}
		certConfig := &tls.Config{Certificates: []tls.Certificate{cert}}
		s.listener, err = tls.Listen("tcp", s.Config.ServerAddress, certConfig)
	} else {
		s.listener, err = net.Listen("tcp", s.Config.ServerAddress)
	}

	if err != nil {
		log.Println("Error listening:", err.Error())
		return err
	}
	log.Println("Listening on " + s.Config.ServerAddress)

	log.Println("Trying to load MqTable...")
	if err = s.LoadMqTable(); err != nil { //load MQ table
		return err
	}
	log.Println("MqTable loaded")

	s.tracker.JoinUpstreams(s.TrackerList)

	for {
		conn, err := s.listener.Accept()
		if err != nil {
			log.Println("Error accepting: ", err.Error())
			return err
		}
		go s.handleConnection(conn)
	}
}

//Close server
func (s *Server) Close() {
	s.listener.Close() //TODO more release may required
}

//LoadMqTable from disk
func (s *Server) LoadMqTable() error {
	if err := EnsureDir(s.Config.MqDir); err != nil {
		log.Printf("MqDir(%s) creation failed:%s", s.Config.MqDir, err.Error())
		return err
	}
	mqTable, err := LoadMqTable(s.Config.MqDir)
	if err != nil {
		log.Println("Error loading MQ table: ", err.Error())
		return err
	}
	for key, val := range mqTable {
		s.MqTable.Set(key, val)
	}
	return nil
}

func (s *Server) serverInfo() *proto.ServerInfo {
	info := &proto.ServerInfo{}
	info.ServerAddress = s.ServerAddress
	info.ServerVersion = proto.VersionValue
	atomic.AddInt64(&s.infoVersion, 1)
	info.InfoVersion = s.infoVersion
	info.TrackerList = s.TrackerList
	info.TopicTable = make(map[string]*proto.TopicInfo)

	s.MqTable.RLock()
	for _, m := range s.MqTable.Map {
		mq, _ := m.(*MessageQueue)
		info.TopicTable[mq.Name()] = mq.TopicInfo()
	}
	s.MqTable.RUnlock()

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

	s.tracker.serverTable.RLock()
	for key, sinfo := range s.tracker.serverTable.Map {
		serverInfo, _ := sinfo.(*proto.ServerInfo)
		info.ServerTable[key] = serverInfo
	}
	s.tracker.serverTable.RUnlock()
	if !s.Config.TrackerOnly {
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

func (s *Server) handleConnection(conn net.Conn) {
	defer conn.Close()
	bufRead := new(bytes.Buffer)
	var wsConn *websocket.Conn
	session := NewSession(&conn, nil)
	s.Created(session)
outter:
	for {
		data := make([]byte, 10240)
		conn.SetReadDeadline(time.Now().Add(s.Config.IdleTimeout))
		n, err := conn.Read(data)
		if err != nil {
			if err, ok := err.(net.Error); ok && err.Timeout() {
				s.OnIdle(session)
				if session.active {
					continue
				} else {
					break
				}
			}

			s.OnError(err, session)
			break
		}
		bufRead.Write(data[0:n])

		for {
			req, err := DecodeMessage(bufRead)
			if err != nil {
				break //message invalid, close
			}
			if req == nil {
				bufRead2 := new(bytes.Buffer)
				bufRead2.Write(bufRead.Bytes())
				bufRead = bufRead2
				break
			}

			//upgrade to Websocket if requested
			if IsWebSocketUpgrade(&req.Header) {
				wsConn, err = s.wsUpgrader.Upgrade(conn, req)
				if err == nil {
					//log.Printf("Upgraded to websocket: %s\n", req)
					session.Upgrade(wsConn)
					break outter
				}
			}
			go s.OnMessage(req, session)
		}
	}

	if wsConn != nil { //upgraded to Websocket
		bufRead = new(bytes.Buffer)
		for {
			_, data, err := wsConn.ReadMessage()
			if err != nil {
				if err, ok := err.(net.Error); ok && err.Timeout() {
					s.OnIdle(session)
					if session.active {
						continue
					} else {
						break
					}
				}
				s.OnError(err, session)
				break
			}
			bufRead.Write(data)
			req, err := DecodeMessage(bufRead)
			if err != nil {
				break //message invalid, close
			}
			if req == nil {
				err = errors.New("Websocket invalid message: " + string(data))
				s.OnError(err, session)
				break
			}
			if IsWebSocketUpgrade(&req.Header) {
				continue
			}
			go s.OnMessage(req, session)
		}
	}
	s.ToDestroy(session)
	conn.Close() //make sure to close the underlying socket
}

//ParseConfig from command line or config file
func ParseConfig() *Config {
	cfg := &Config{}
	cfg.CertFileTable = make(map[string]string)
	var idleTime int

	flag.StringVar(&cfg.ServerAddress, "serverAddress", "0.0.0.0:15555", "Server address")
	flag.StringVar(&cfg.ServerName, "serverName", "", "Server public server name, e.g. zbus.io")
	flag.IntVar(&idleTime, "idleTimeout", 180, "Idle detection timeout in seconds") //default to 3 minute
	flag.StringVar(&cfg.MqDir, "mqDir", "/tmp/zbus", "Message Queue directory")
	flag.StringVar(&cfg.LogDir, "logDir", "", "Log file location")
	flag.StringVar(&cfg.TrackerList, "trackerList", "", "Tracker list, e.g.: localhost:15555;localhost:15556")
	flag.BoolVar(&cfg.TrackerOnly, "trackerOnly", false, "True--Work as Tracker only, False--MqServer+Tracker")
	flag.BoolVar(&cfg.SslEnabled, "sslEnabled", false, "Enable SSL")
	flag.StringVar(&cfg.ServerCertFile, "serverCertFile", "", "Server certificate file full path")
	flag.StringVar(&cfg.ServerCertKey, "serverCertKey", "", "Server certificate key path")
	flag.StringVar(&cfg.CertFileDir, "certFileDir", "", "Client certificate directory to lookup, when connecting to other servers")
	flag.StringVar(&cfg.DefaultCertFile, "defaultCertFile", "", "Client certificate directory to lookup, when connecting to other servers")

	flag.Parse()
	cfg.IdleTimeout = time.Duration(idleTime)

	var configFile string
	if flag.NArg() == 1 { //if only one argument, assume to be configuration file,
		configFile = flag.Args()[0]
		jsonData, err := ioutil.ReadFile(configFile)
		if err != nil {
			log.Fatalf("Read config file error: %s", err.Error())
			os.Exit(-1)
		}

		err = json.Unmarshal(jsonData, cfg)
		if err != nil {
			log.Fatalf("Read config file error: %s", err.Error())
			os.Exit(-1)
		}
	}

	cfg.IdleTimeout = time.Duration(cfg.IdleTimeout) * time.Second
	return cfg
}

func main() {
	log.SetFlags(log.Lshortfile | log.Ldate | log.Ltime)
	printBanner()

	config := ParseConfig()
	var logTargets []io.Writer
	if config.LogToConsole {
		logTargets = append(logTargets, os.Stdout)
	}
	if config.LogDir != "" {
	}
	if logTargets != nil {
		w := io.MultiWriter(logTargets...)
		log.SetOutput(w)
	}

	server := NewServer(config)
	server.Start()
}

func printBanner() {
	logo := fmt.Sprintf(`
                /\\\       
                \/\\\        
                 \/\\\    
     /\\\\\\\\\\\ \/\\\         /\\\    /\\\  /\\\\\\\\\\     
     \///////\\\/  \/\\\\\\\\\  \/\\\   \/\\\ \/\\\//////     
           /\\\/    \/\\\////\\\ \/\\\   \/\\\ \/\\\\\\\\\\    
          /\\\/      \/\\\  \/\\\ \/\\\   \/\\\ \////////\\\  
         /\\\\\\\\\\\ \/\\\\\\\\\  \//\\\\\\\\\   /\\\\\\\\\\
         \///////////  \/////////    \/////////   \//////////     Version: %s
		`, proto.VersionValue)
	fmt.Println(logo)
}
