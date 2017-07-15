package main

import (
	"encoding/json"
	"fmt"
	"testing"

	"./proto"
)

func TestNewMqClient(t *testing.T) {
	c := NewMqClient("localhost:15555", nil)

	err := c.Connect()

	if err != nil {
		t.Fail()
	}

	c.Close()
}

func TestMqClient_Send(t *testing.T) {
	c := NewMqClient("localhost:15555", nil)
	defer c.Close()

	var err error
	req := NewMessage()
	req.Url = "/server"

	err = c.Send(req)
	println(req.Id())
	if err != nil {
		t.Fail()
	}

	msg, err := c.Recv(nil)
	if err != nil {
		t.Fail()
	}

	println(msg.String())
}

func TestMqClient_Timeout(t *testing.T) {
	c := NewMqClient("localhost:15555", nil)
	defer c.Close()

	_, err := c.Recv(nil)
	fmt.Println(err)
}

func TestMqClient_Invoke(t *testing.T) {
	c := NewMqClient("localhost:15555", nil)
	msg := NewMessage()
	msg.SetCmd(proto.Tracker)

	resp, err := c.Invoke(msg)
	if err != nil {
		t.Fail()
	}
	fmt.Println(resp)
}

func TestMqClient_Close(t *testing.T) {
	c := NewMqClient("localhost:15550", nil)

	notify := make(chan bool)
	c.EnsureConnected(notify)

	c.Close()
	<-notify
}

func TestMqClient_QueryTracker(t *testing.T) {
	c := NewMqClient("localhost:15555", nil)
	info, err := c.QueryTracker()
	if err != nil {
		println(err)
		t.Fail()
	}
	s, _ := json.Marshal(info)
	fmt.Println(string(s))
	c.Close()
}

func TestMqClient_QueryServer(t *testing.T) {
	c := NewMqClient("localhost:15555", nil)
	info, err := c.QueryServer()
	if err != nil {
		println(err)
		t.Fail()
	}
	s, _ := json.Marshal(info)
	fmt.Println(string(s))
	c.Close()
}

func TestMqClient_QueryTopic(t *testing.T) {
	c := NewMqClient("localhost:15555", nil)
	info, err := c.QueryTopic("hong")
	if err != nil {
		println(err)
		t.Fail()
	}
	s, _ := json.Marshal(info)
	fmt.Println(string(s))
	c.Close()
}

func TestMqClient_QueryConsumeGroup(t *testing.T) {
	c := NewMqClient("localhost:15555", nil)
	info, err := c.QueryGroup("hong", "hong")
	if err != nil {
		println(err)
		t.Fail()
	}
	s, _ := json.Marshal(info)
	fmt.Println(string(s))
	c.Close()
}

func TestMqClient_DeclareTopic(t *testing.T) {
	c := NewMqClient("localhost:15555", nil)
	info, err := c.DeclareTopic("hongx", nil)
	if err != nil {
		println(err)
		t.Fail()
	}
	s, _ := json.Marshal(info)
	fmt.Println(string(s))
	c.Close()
}

func TestMqClient_DeclareGroup(t *testing.T) {
	c := NewMqClient("localhost:15555", nil)
	g := &ConsumeGroup{}
	g.GroupName = "mygroup"
	info, err := c.DeclareGroup("hongx", g)
	if err != nil {
		println(err)
		t.Fail()
	}
	s, _ := json.Marshal(info)
	fmt.Println(string(s))
	c.Close()
}

func TestMqClient_RemoveTopic(t *testing.T) {
	c := NewMqClient("localhost:15555", nil)

	err := c.RemoveTopic("hongx")
	if err != nil {
		println(err)
		t.Fail()
	}

	c.Close()
}
func TestMqClient_RemoveGroup(t *testing.T) {
	c := NewMqClient("localhost:15555", nil)

	err := c.RemoveGroup("hongx", "mygroup")
	if err != nil {
		println(err)
		t.Fail()
	}

	c.Close()
}

func TestMqClient_Produce(t *testing.T) {
	c := NewMqClient("localhost:15555", nil)
	req := NewMessage()
	req.SetTopic("hong")
	req.SetBodyString("From Go")

	resp, err := c.Produce(req)
	if err != nil {
		println(err)
		t.Fail()
	}
	println(resp.String())
	c.Close()
}

func TestMqClient_Consume(t *testing.T) {
	c := NewMqClient("localhost:15555", nil)
	resp, err := c.Consume("hong", nil, nil)
	if err != nil {
		println(err)
		t.Fail()
	}
	if resp != nil {
		println(resp.String())
	}
	c.Close()
}
