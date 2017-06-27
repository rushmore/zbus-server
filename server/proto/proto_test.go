package proto

import (
	"encoding/json"
	"fmt"
	"testing"
)

func TestJson(t *testing.T) {
	g := &ConsumeGroupInfo{}
	g.GroupName = "MyGroup"
	g.MessageCount = 0

	data, err := json.Marshal(g)
	if err != nil {
		t.Fail()
	}
	println(string(data))

	track := &TrackerInfo{}
	data, err = json.Marshal(track)
	if err != nil {
		t.Fail()
	}
	println(string(data))

}

func TestAddServerContext(t *testing.T) {
	topicInfo := &TopicInfo{}
	addr := &ServerAddress{"localhost:15555", false}

	AddServerContext(topicInfo, addr)

	fmt.Println(topicInfo)
}
