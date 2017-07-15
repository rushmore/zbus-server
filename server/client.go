package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"log"
	"net"
	"os"
	"sync"
	"time"

	"./proto"
)

//MqClient TCP client using Message type
type MqClient struct {
	conn       *net.TCPConn
	address    string
	sslEnabled bool
	certFile   string
	bufRead    *bytes.Buffer

	msgTable      SyncMap //map[string]*Message
	timeout       time.Duration
	autoReconnect bool
	mutex         sync.Mutex

	heartbeatInterval time.Duration
	stopHeartbeat     chan bool

	onConnected    func(*MqClient)
	onDisconnected func(*MqClient)
	onMessage      func(*MqClient, *Message)

	token string
}

//NewMqClient create message client, if sslEnabled, certFile should be provided
func NewMqClient(address string, certFile *string) *MqClient {
	c := &MqClient{}
	c.address = address
	c.bufRead = new(bytes.Buffer)
	c.timeout = 3000 * time.Millisecond
	c.heartbeatInterval = 30 * time.Second

	c.msgTable.Map = make(map[string]interface{})
	if certFile != nil {
		c.sslEnabled = true
		c.certFile = *certFile
	}
	go c.heartbeat() //start heatbeat by default
	return c
}

func (c *MqClient) heartbeat() {
hearbeat:
	for {
		select {
		case <-time.After(c.heartbeatInterval):
		case <-c.stopHeartbeat:
			break hearbeat
		}

		if c.conn == nil {
			continue
		}
		msg := NewMessage()
		msg.SetCmd(proto.Heartbeat)

		err := c.Send(msg)
		if err != nil {
			log.Printf("Sending heartbeat error: %s", err.Error())
		}
	}
}

//Connect to server
func (c *MqClient) Connect() error {
	if c.conn != nil {
		return nil
	}
	c.mutex.Lock()
	if c.conn != nil {
		c.mutex.Unlock()
		return nil
	}

	log.Printf("Trying connect to %s\n", c.address)
	conn, err := net.DialTimeout("tcp", c.address, c.timeout)
	if err != nil {
		c.mutex.Unlock()
		return err
	}
	c.mutex.Unlock()
	c.conn = conn.(*net.TCPConn)
	if c.onConnected != nil {
		c.onConnected(c)
	} else {
		log.Printf("Connected to %s\n", c.address)
	}
	return nil
}

//Close client
func (c *MqClient) Close() {
	c.autoReconnect = false
	select {
	case c.stopHeartbeat <- true:
	default:
	}
	c.closeConn()
}

func (c *MqClient) closeConn() {
	if c.conn == nil {
		return
	}
	c.conn.Close()
	c.conn = nil
}

//SetToken set security token
func (c *MqClient) SetToken(token string) {
	c.token = token
}

//Invoke message to server and get reply matching msgid
func (c *MqClient) Invoke(req *Message) (*Message, error) {
	err := c.Send(req)
	if err != nil {
		return nil, err
	}
	msgid := req.Id()
	return c.Recv(&msgid)
}

//Send Message
func (c *MqClient) Send(req *Message) error {
	err := c.Connect() //connect if needs
	if err != nil {
		return err
	}

	if req.Id() == "" {
		req.SetId(uuid())
	}
	buf := new(bytes.Buffer)
	req.EncodeMessage(buf)

	data := buf.Bytes()
	for {
		n, err := c.conn.Write(data)
		if err != nil {
			return err
		}
		if n >= len(data) {
			break
		}
		data = data[n:]
	}
	return nil
}

//Recv Message
func (c *MqClient) Recv(msgid *string) (*Message, error) {
	err := c.Connect() //connect if needs
	if err != nil {
		return nil, err
	}
	for {
		if msgid != nil {
			msg, _ := c.msgTable.Get(*msgid).(*Message)
			if msg != nil {
				c.msgTable.Remove(*msgid)
				return msg, nil
			}
		}
		data := make([]byte, 10240)
		c.conn.SetReadDeadline(time.Now().Add(c.timeout))
		n, err := c.conn.Read(data)
		if err != nil {
			return nil, err
		}
		c.bufRead.Write(data[0:n])
		resp, err := DecodeMessage(c.bufRead)
		if err != nil {
			return nil, err
		}
		if resp == nil {
			bufRead2 := new(bytes.Buffer)
			bufRead2.Write(c.bufRead.Bytes())
			c.bufRead = bufRead2
			continue
		}

		respId := resp.Id()
		if msgid == nil || respId == "" || respId == *msgid {
			return resp, nil
		}
		c.msgTable.Set(respId, resp)
	}
}

//EnsureConnected trying to connect the client util success
func (c *MqClient) EnsureConnected(notify chan bool) {
	go func() {
		for {
			err := c.Connect()
			if err == nil {
				break
			}
			log.Printf("Connection to(%s) failed: %s", c.address, err.Error())
			time.Sleep(c.timeout)
		}
		if notify != nil {
			notify <- true
		}
	}()
}

//Start a goroutine to recv message from server
func (c *MqClient) Start(notify chan bool) {
	c.autoReconnect = true
	go func() {
	for_loop:
		for {
			msg, err := c.Recv(nil)
			if err == nil {
				if c.onMessage != nil {
					c.onMessage(c, msg)
				}
				continue
			}

			if err, ok := err.(net.Error); ok && err.Timeout() {
				continue
			}
			c.closeConn()
			if c.onDisconnected != nil {
				c.onDisconnected(c)
			}
			if !c.autoReconnect {
				break for_loop
			}

			time.Sleep(c.timeout)
			switch err.(type) {
			case *net.OpError:
			case *os.SyscallError:
			default:
				break for_loop
			}
		}
		if notify != nil {
			notify <- true
		}
	}()
}

func (c *MqClient) invokeCmd(req *Message, info interface{}) error {
	req.SetToken(c.token)
	resp, err := c.Invoke(req)
	if err != nil {
		return err
	}
	if resp.Status != 200 {
		err = fmt.Errorf("Status=%d, Error=%s", resp.Status, string(resp.body))
		errInfo, _ := info.(*proto.ErrInfo)
		if errInfo != nil {
			errInfo.Error = err
		} else {
			return err
		}
	} else {
		if info != nil {
			err = json.Unmarshal(resp.body, info)
			if err != nil {
				return err
			}
		}
	}
	return nil
}

//QueryTracker read tracker info from Tracker
func (c *MqClient) QueryTracker() (*proto.TrackerInfo, error) {
	req := NewMessage()
	req.SetCmd(proto.Tracker)

	info := &proto.TrackerInfo{}
	err := c.invokeCmd(req, info)
	if err != nil {
		return nil, err
	}
	return info, err
}

//QueryServer read server info from MqServer
func (c *MqClient) QueryServer() (*proto.ServerInfo, error) {
	req := NewMessage()
	req.SetCmd(proto.Server)

	info := &proto.ServerInfo{}
	err := c.invokeCmd(req, info)
	if err != nil {
		return nil, err
	}
	return info, err
}

//QueryTopic read topic info from MqServer
func (c *MqClient) QueryTopic(topic string) (*proto.TopicInfo, error) {
	req := NewMessage()
	req.SetCmd(proto.Query)
	req.SetTopic(topic)

	info := &proto.TopicInfo{}
	err := c.invokeCmd(req, info)
	if err != nil {
		return nil, err
	}
	return info, err
}

//QueryGroup read consume-group info from MqServer
func (c *MqClient) QueryGroup(topic string, group string) (*proto.ConsumeGroupInfo, error) {
	req := NewMessage()
	req.SetCmd(proto.Query)
	req.SetTopic(topic)
	req.SetConsumeGroup(group)

	info := &proto.ConsumeGroupInfo{}
	err := c.invokeCmd(req, info)
	if err != nil {
		return nil, err
	}
	return info, err
}

//DeclareTopic creator or update topic
func (c *MqClient) DeclareTopic(topic string, mask *int32) (*proto.TopicInfo, error) {
	req := NewMessage()
	req.SetCmd(proto.Declare)
	req.SetTopic(topic)
	if mask != nil {
		req.SetTopicMask(*mask)
	}

	info := &proto.TopicInfo{}
	err := c.invokeCmd(req, info)
	if err != nil {
		return nil, err
	}
	return info, err
}

//DeclareGroup creator or update consume-group
func (c *MqClient) DeclareGroup(topic string, group *ConsumeGroup) (*proto.ConsumeGroupInfo, error) {
	req := NewMessage()
	req.SetCmd(proto.Declare)
	req.SetTopic(topic)
	if group != nil {
		group.WriteTo(req)
	} else {
		req.SetConsumeGroup(topic) //default to topic name
	}

	info := &proto.ConsumeGroupInfo{}
	err := c.invokeCmd(req, info)
	if err != nil {
		return nil, err
	}
	return info, err
}

//RemoveTopic remove topic
func (c *MqClient) RemoveTopic(topic string) error {
	req := NewMessage()
	req.SetCmd(proto.Remove)
	req.SetTopic(topic)

	err := c.invokeCmd(req, nil)
	return err
}

//RemoveGroup remove consume-group
func (c *MqClient) RemoveGroup(topic string, group string) error {
	req := NewMessage()
	req.SetCmd(proto.Remove)
	req.SetTopic(topic)
	req.SetConsumeGroup(group)

	err := c.invokeCmd(req, nil)
	return err
}

//EmptyTopic clear message in topic
func (c *MqClient) EmptyTopic(topic string) error {
	req := NewMessage()
	req.SetCmd(proto.Empty)
	req.SetTopic(topic)

	err := c.invokeCmd(req, nil)
	return err
}

//EmptyGroup clear message in consume-group
func (c *MqClient) EmptyGroup(topic string, group string) error {
	req := NewMessage()
	req.SetCmd(proto.Empty)
	req.SetTopic(topic)
	req.SetConsumeGroup(group)

	err := c.invokeCmd(req, nil)
	return err
}

//Produce message to zbus server
//request set Ack false, get nil returned
func (c *MqClient) Produce(req *Message) (*Message, error) {
	req.SetCmd(proto.Produce)
	if req.Ack() {
		return c.Invoke(req)
	}
	return nil, c.Send(req)
}

//Consume consumes a message from zbus
func (c *MqClient) Consume(topic string, group *string, window *int32) (*Message, error) {
	req := NewMessage()
	req.SetCmd(proto.Consume)
	req.SetTopic(topic)
	if group != nil {
		req.SetConsumeGroup(*group)
	}
	if window != nil {
		req.SetWindow(*window)
	}

	resp, err := c.Invoke(req)
	if err != nil {
		return nil, err
	}
	resp.SetId(resp.OriginId())
	resp.RemoveHeader(proto.OriginId)
	if resp.Status == 200 {
		url := resp.OriginUrl()
		if url != "" {
			resp.Url = url
			resp.Status = -1
			resp.RemoveHeader(proto.OriginUrl)
		}
	}
	return resp, nil
}

//Route sends route message to zbus to target client, for RPC purpose
func (c *MqClient) Route(msg *Message) error {
	msg.SetCmd(proto.Route)
	if msg.Status > 0 {
		msg.SetOriginStatus(msg.Status)
		msg.Status = -1 //make it as request
	}
	return c.Send(msg)
}
