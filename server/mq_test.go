package main

import (
	"encoding/json"
	"fmt"
	"testing"
)

var mq, _ = NewMessageQueue("/tmp/diskq", "hong")

func TestNewMessageQueue(t *testing.T) {
	q, err := NewMessageQueue("/tmp/diskq", "hong")
	if err != nil {
		t.Fail()
	}
	defer q.Close()
}

func TestMessageQueue_Write(t *testing.T) {
	msg := NewMessage()
	msg.Header["cmd"] = "produce"
	msg.SetBodyString("hello world")
	err := mq.Write(msg)
	if err != nil {
		t.Fail()
	}
}

func TestMessageQueue_Read(t *testing.T) {

	msg, _, err := mq.Read("hong")
	if err != nil {
		t.Fail()
	}
	if msg != nil {
		fmt.Println(msg)
	}
}

func TestMessageQueue_ConsumeGroup(t *testing.T) {
	g := &ConsumeGroup{}
	g.GroupName = "hong"
	g.Mask = &[]int32{0}[0]
	fmt.Println(g.Mask)
}
func TestLoadMqTable(t *testing.T) {
	table, err := LoadMqTable("/tmp/diskq")
	if err != nil {
		t.Fail()
	}
	println(table)
}

func TestMessageQueue_DeclareGroup(t *testing.T) {
	group := &ConsumeGroup{}
	group.GroupName = "hongx"

	info, err := mq.DeclareGroup(group)
	if err != nil {
		t.Fail()
	}
	fmt.Println(info)
}

func TestMessageQueue_TopicInfo(t *testing.T) {
	info := mq.TopicInfo()
	data, _ := json.MarshalIndent(info, "", "	")
	println(string(data))
}

func TestMessageQueue_Destroy(t *testing.T) {
	mq, _ := NewMessageQueue("/tmp/tempmq", "hong2")
	err := mq.Destroy()
	if err != nil {
		t.Fail()
	}
}
