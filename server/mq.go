package main

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"path/filepath"
	"strings"

	"./diskq"
	"./proto"
)

//MessageQueue writer + N readers
type MessageQueue struct {
	index    *diskq.Index
	name     string
	writer   *diskq.QueueWriter //for Producer to write
	readers  SyncMap            //also known as ConsumeGroup, string=>*diskq.QueueReader
	avaiable chan bool
}

//LoadMqTable load MQ from based directory
func LoadMqTable(baseDir string) (map[string]*MessageQueue, error) {
	files, err := ioutil.ReadDir(baseDir)
	if err != nil {
		return nil, err
	}

	table := make(map[string]*MessageQueue)
	for _, file := range files {
		if !file.IsDir() {
			continue //ignore
		}
		fileName := file.Name()
		idxFilePath := filepath.Join(baseDir, fileName, fmt.Sprintf("%s%s", fileName, diskq.IndexSuffix))
		if _, err = os.Stat(idxFilePath); err != nil {
			continue //invalid directory
		}
		q, err := NewMessageQueue(baseDir, fileName)
		if err != nil {
			log.Printf("Load MQ(%s) failed, error: %s", fileName, err)
			continue
		}
		table[fileName] = q
	}

	return table, nil
}

//NewMessageQueue create a message queue
func NewMessageQueue(baseDir string, name string) (*MessageQueue, error) {
	dirPath := filepath.Join(baseDir, name)
	index, err := diskq.NewIndex(dirPath)
	if err != nil {
		return nil, err
	}
	q := &MessageQueue{}
	q.avaiable = make(chan bool)
	q.index = index
	q.name = q.index.Name()
	q.writer, err = diskq.NewQueueWriter(index)
	if err != nil {
		q.Close()
		return nil, err
	}
	q.readers.Map = make(map[string]interface{})
	err = q.loadReaders()
	if err != nil {
		q.Close()
		return nil, err
	}
	return q, nil
}

//Close disk queue
func (q *MessageQueue) Close() {
	if q.writer != nil {
		q.writer.Close()
		q.writer = nil
	}
	q.readers.Lock()
	defer q.readers.Unlock()
	for _, g := range q.readers.Map {
		r := g.(*diskq.QueueReader)
		r.Close()
	}
	if q.index != nil {
		q.index.Close()
		q.index = nil
	}
	q.readers.Map = nil
}

//Produce a message to MQ
func (q *MessageQueue) Write(msg *Message) error {
	data := &diskq.DiskMsg{}
	data.Id = msg.Id()
	data.Tag = msg.Tag()
	buf := new(bytes.Buffer)
	msg.EncodeMessage(buf)
	data.Body = buf.Bytes()

	_, err := q.writer.Write(data)

	q.readers.Lock()
	defer q.readers.Unlock()
	for _, g := range q.readers.Map {
		r := g.(*diskq.QueueReader)
		select {
		case r.Available <- true:
		default: //ignore
		}
	}
	return err
}

//WriteBatch write multiple message in on batch
func (q *MessageQueue) WriteBatch(msgs []*Message) error {
	if len(msgs) <= 0 {
		return nil
	}

	dmsgs := make([]*diskq.DiskMsg, len(msgs))
	for i := 0; i < len(msgs); i++ {
		msg := msgs[i]
		data := &diskq.DiskMsg{}
		data.Id = msg.Id()
		data.Tag = msg.Tag()
		buf := new(bytes.Buffer)
		msg.EncodeMessage(buf)
		data.Body = buf.Bytes()
		dmsgs[i] = data
	}
	_, err := q.writer.WriteBatch(dmsgs)
	return err
}

//Read a message from MQ' consume group
func (q *MessageQueue) Read(group string) (*Message, int, error) {
	if group == "" {
		group = q.name //default to topic name
	}
	r, _ := q.readers.Get(group).(*diskq.QueueReader)
	if r == nil {
		return nil, 404, fmt.Errorf("ConsumeGroup(%s) not found", group)
	}
	data, err := r.Read()
	if err != nil {
		return nil, 500, err
	}
	if data == nil {
		<-r.Available //wait for signal
		return q.Read(group)
	}
	buf := bytes.NewBuffer(data.Body)
	return DecodeMessage(buf), 200, nil
}

//ConsumeGroup returns reader for the consume group
func (q *MessageQueue) ConsumeGroup(group string) *diskq.QueueReader {
	g, _ := q.readers.Get(group).(*diskq.QueueReader)
	return g
}

//DeclareGroup create/update a consume group
func (q *MessageQueue) DeclareGroup(group *ConsumeGroup) (*proto.ConsumeGroupInfo, error) {
	groupName := group.GroupName
	if groupName == "" {
		groupName = q.name
	}
	g, _ := q.readers.Get(groupName).(*diskq.QueueReader)
	if g == nil { //Create new consume group reader
		var g2 *diskq.QueueReader
		var err error
		if group.StartCopy != nil {
			g2, _ = q.readers.Get(*group.StartCopy).(*diskq.QueueReader)
		}
		if g2 == nil {
			g2 = q.findLatesReader()
		}
		if g2 == nil {
			g, err = diskq.NewQueueReader(q.index, groupName)
			if err != nil {
				return nil, err
			}
		} else { //copy g2
			g, err = diskq.NewQueueReaderCopy(g2, groupName)
			if err != nil {
				return nil, err
			}
		}
		q.readers.Set(groupName, g)
	}

	if group.Filter != nil {
		g.SetFilter(*group.Filter)
	}
	if group.Mask != nil {
		g.SetMask(*group.Mask)
	}
	//TODO SEEK by start

	return q.groupInfo(g), nil
}

//RemoveGroup remove a consume group
func (q *MessageQueue) RemoveGroup(group string) error {
	g := q.readers.Remove(group)
	if g == nil {
		return nil
	}
	r, _ := g.(*diskq.QueueReader)
	groupFile := r.File()
	r.Close()
	return os.Remove(groupFile)
}

//Destroy MQ
func (q *MessageQueue) Destroy() error {
	dir := q.index.Dir()
	q.Close()
	return os.RemoveAll(dir)
}

//TopicInfo returns message queue info
func (q *MessageQueue) TopicInfo() *proto.TopicInfo {
	info := &proto.TopicInfo{}
	info.TopicName = q.name
	info.Mask = q.index.Mask()
	info.MessageDepth = q.index.MsgNo()
	info.Creator = q.index.Creator()
	info.CreatedTime = q.index.CreatedTime()
	info.LastUpdatedTime = q.index.UpdatedTime()
	info.ConsumeGroupList = []*proto.ConsumeGroupInfo{}
	q.readers.RLock()
	defer q.readers.RUnlock()
	for _, g := range q.readers.Map {
		r, _ := g.(*diskq.QueueReader)
		groupInfo := q.groupInfo(r)
		info.ConsumeGroupList = append(info.ConsumeGroupList, groupInfo)
	}
	return info
}

//GroupInfo returns consume group info
func (q *MessageQueue) GroupInfo(group string) *proto.ConsumeGroupInfo {
	g, _ := q.readers.Get(group).(*diskq.QueueReader)
	if g != nil {
		return q.groupInfo(g)
	}
	return nil
}

func (q *MessageQueue) groupInfo(g *diskq.QueueReader) *proto.ConsumeGroupInfo {
	info := &proto.ConsumeGroupInfo{}
	info.TopicName = q.name
	info.GroupName = g.Name()
	info.Mask = g.Mask()
	info.Filter = g.Filter()
	info.MessageCount = g.MsgCount()
	info.Creator = g.Creator()
	info.CreatedTime = g.CreatedTime()
	info.LastUpdatedTime = g.UpdatedTime()

	//info.ConsumerCount and info.ConsumerList missing
	return info
}

func (q *MessageQueue) findLatesReader() *diskq.QueueReader {
	var t *diskq.QueueReader
	q.readers.RLock()
	defer q.readers.RUnlock()
	for _, g := range q.readers.Map {
		r, _ := g.(*diskq.QueueReader)

		if t == nil {
			t = r
			continue
		}

		if r.BlockNo() < t.BlockNo() {
			continue
		}
		if r.BlockNo() > t.BlockNo() {
			t = r
			continue
		}
		if r.MsgNo() >= t.MsgNo() {
			t = r
		}
	}
	return t
}
func (q *MessageQueue) loadReaders() error {
	readerDir := q.index.ReaderDir()
	if err := os.MkdirAll(readerDir, 0777); err != nil {
		return err
	}
	files, err := ioutil.ReadDir(readerDir)
	if err != nil {
		return err
	}
	for _, file := range files {
		if file.IsDir() {
			continue //ignore
		}
		fileName := file.Name()
		if !strings.HasSuffix(fileName, diskq.ReaderSuffix) {
			continue
		}
		name := fileName[0 : len(fileName)-len(diskq.ReaderSuffix)]
		r, err := diskq.NewQueueReader(q.index, name)
		if err != nil {
			log.Printf("Reader %s load error: %s", fileName, err)
		}
		q.readers.Set(name, r)
	}
	return nil
}

//Name return the name of MQ
func (q *MessageQueue) Name() string {
	return q.name
}

//SetMask update mask value
func (q *MessageQueue) SetMask(value int32) {
	q.index.SetMask(value)
}

//SetCreator update creator
func (q *MessageQueue) SetCreator(value string) {
	q.index.SetCreator(value)
}

//SetExt update ext
func (q *MessageQueue) SetExt(i int, value string) error {
	return q.index.SetExt(i, value)
}

//GetExt get ext
func (q *MessageQueue) GetExt(i int) (string, error) {
	return q.index.GetExt(i)
}

//ConsumeGroup consume group info
type ConsumeGroup struct {
	GroupName string
	Filter    *string //filter on message'tag
	Mask      *int32

	StartCopy   *string //create group from another group
	StartOffset *int64
	StartMsgid  *string //create group start from offset, msgId to validate
	StartTime   *int64  //create group start from time

	//only used in server side, TODO
	Creator *string
}

//WriteTo message
func (g *ConsumeGroup) WriteTo(m *Message) {
	m.SetConsumeGroup(g.GroupName)
	if g.Filter != nil {
		m.SetGroupFilter(*g.Filter)
	}
	if g.Mask != nil {
		m.SetGroupMask(*g.Mask)
	}
	if g.StartCopy != nil {
		m.SetGroupStartCopy(*g.StartCopy)
	}
	if g.StartMsgid != nil {
		m.SetGroupStartMsgid(*g.StartMsgid)
	}
	if g.StartOffset != nil {
		m.SetGroupStartOffset(*g.StartOffset)
	}
	if g.StartTime != nil {
		m.SetGroupStartTime(*g.StartTime)
	}
}

//LoadFrom message
func (g *ConsumeGroup) LoadFrom(m *Message) {
	g.GroupName = m.ConsumeGroup()
	g.Filter = m.GroupFilter()
	g.Mask = m.GroupMask()
	g.StartCopy = m.GroupStartCopy()
	g.StartMsgid = m.GroupStartMsgid()
	g.StartOffset = m.GroupStartOffset()
	g.StartTime = m.GroupStartTime()
}
