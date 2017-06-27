package main

import (
	"bytes"
	"log"
	"net"
	"os"
	"sync"
	"time"
)

//MessageClient TCP client using Message type
type MessageClient struct {
	conn       *net.TCPConn
	address    string
	sslEnabled bool
	certFile   string
	bufRead    *bytes.Buffer

	msgTable      map[string]*Message
	timeout       time.Duration
	autoReconnect bool
	mutex         *sync.Mutex

	onConnected    func(*MessageClient)
	onDisconnected func(*MessageClient)
	onMessage      func(*MessageClient, *Message)
}

//NewMessageClient create message client, if sslEnabled, certFile should be provided
func NewMessageClient(address string, certFile *string) *MessageClient {
	c := &MessageClient{}
	c.address = address
	c.bufRead = new(bytes.Buffer)
	c.msgTable = make(map[string]*Message)
	c.timeout = 3000 * time.Millisecond
	c.mutex = &sync.Mutex{}
	if certFile != nil {
		c.sslEnabled = true
		c.certFile = *certFile
	}
	return c
}

//Connect to server
func (c *MessageClient) Connect() error {
	if c.conn != nil {
		return nil
	}
	c.mutex.Lock()
	defer c.mutex.Unlock()
	if c.conn != nil {
		return nil
	}

	log.Printf("Trying connect to %s\n", c.address)
	conn, err := net.DialTimeout("tcp", c.address, c.timeout)
	if err != nil {
		return err
	}
	c.conn = conn.(*net.TCPConn)
	if c.onConnected != nil {
		c.onConnected(c)
	} else {
		log.Printf("Connected to %s\n", c.address)
	}
	return nil
}

//Close client
func (c *MessageClient) Close() {
	c.autoReconnect = false
	c.close()
}

func (c *MessageClient) close() {
	if c.conn == nil {
		return
	}
	c.conn.Close()
	c.conn = nil
}

//Invoke message to server and get reply matching msgid
func (c *MessageClient) Invoke(req *Message) (*Message, error) {
	err := c.Send(req)
	if err != nil {
		return nil, err
	}
	msgid := req.Id()
	return c.Recv(&msgid)
}

//Send Message
func (c *MessageClient) Send(req *Message) error {
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
func (c *MessageClient) Recv(msgid *string) (*Message, error) {
	err := c.Connect() //connect if needs
	if err != nil {
		return nil, err
	}
	for {
		if msgid != nil {
			msg := c.msgTable[*msgid]
			if msg != nil {
				delete(c.msgTable, *msgid)
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
		resp := DecodeMessage(c.bufRead)
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
		c.msgTable[respId] = resp
	}
}

//EnsureConnected trying to connect the client util success
func (c *MessageClient) EnsureConnected(notify chan bool) {
	go func() {
		for {
			err := c.Connect()
			if err == nil {
				break
			}
			time.Sleep(c.timeout)
		}
		if notify != nil {
			notify <- true
		}
	}()
}

//Start a goroutine to recv message from server
func (c *MessageClient) Start(notify chan bool) {
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
			c.close()
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
