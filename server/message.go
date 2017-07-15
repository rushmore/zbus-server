package main

import (
	"bytes"
	"fmt"
	"strconv"
	"strings"

	"./proto"
)

var statusText = map[int]string{
	100: "Continue",
	101: "Switching Protocols",
	200: "OK",
	400: "Bad Request",
	401: "Unauthorized",
	403: "Forbidden",
	404: "Not Found",
	405: "Method Not Allowed",
	500: "Internal Server Error",
	502: "Bad Gateway",
	503: "Service Unavailable",
	504: "Gateway Timeout",
}

// StatusText returns a text for the HTTP status code.
func StatusText(code int) string {
	value, ok := statusText[code]
	if !ok {
		return "Unkown Status"
	}
	return value
}

//Message stands for HTTP message including Request and Response
type Message struct {
	Status int
	Url    string
	Method string
	Header SyncMap

	body []byte
}

//NewMessage creates a Message, args if provied format to body string
func NewMessage(args ...interface{}) *Message {
	m := new(Message)
	m.Url = "/"
	m.Method = "GET"
	m.Header.Map = make(map[string]interface{})
	m.Header.Set("connection", "Keep-Alive")
	m.SetBodyString(args...)
	return m
}

//NewMessageStatus create message with status and body
func NewMessageStatus(status int, args ...interface{}) *Message {
	m := NewMessage(args...)
	m.Status = status
	return m
}

//SetHeaderIfNone updates header by value if not set yet
func (m *Message) SetHeaderIfNone(key string, val string) {
	if m.Header.Contains(key) {
		return
	}
	m.Header.Set(key, val)
}

//Body returns body as string, nil as "" empty string
func (m *Message) Body() string {
	if m.body == nil {
		return ""
	}
	return string(m.body)
}

//SetBody set binary body of Message
func (m *Message) SetBody(body []byte) {
	m.body = body
}

//SetBodyString set string body of Message
func (m *Message) SetBodyString(args ...interface{}) {
	if len(args) > 0 {
		format := args[0]
		switch format.(type) {
		case string:
			body := fmt.Sprintf(format.(string), args[1:]...)
			m.body = []byte(body)
			//ignore otherwise
		}
	}
}

//SetJsonBody set json body
func (m *Message) SetJsonBody(body string) {
	m.body = []byte(body)
	m.Header.Set("content-type", "application/json")
}

//EncodeMessage encodes Message to []byte
func (m *Message) EncodeMessage(buf *bytes.Buffer) {
	if m.Status > 0 {
		buf.WriteString(fmt.Sprintf("HTTP/1.1 %d %s\r\n", m.Status, StatusText(m.Status)))
	} else {
		buf.WriteString(fmt.Sprintf("%s %s HTTP/1.1\r\n", m.Method, m.Url))
	}

	m.Header.RLock()
	for k, v := range m.Header.Map {
		k = strings.ToLower(k)
		if k == "content-length" {
			continue
		}
		val, ok := v.(string)
		if ok {
			buf.WriteString(fmt.Sprintf("%s: %s\r\n", k, val))
		}
	}
	m.Header.RUnlock()

	bodyLen := 0
	if m.body != nil {
		bodyLen = len(m.body)
	}
	buf.WriteString(fmt.Sprintf("content-length: %d\r\n", bodyLen))

	buf.WriteString("\r\n")
	if m.body != nil {
		buf.Write(m.body)
	}
}

//String convert message to string
func (m *Message) String() string {
	buf := new(bytes.Buffer)
	m.EncodeMessage(buf)
	return string(buf.Bytes())
}

//DecodeMessage decode Message from Buffer, nil returned if not enought in buffer
func DecodeMessage(buf *bytes.Buffer) (*Message, error) {
	bb := buf.Bytes()
	idx := bytes.Index(bb, []byte("\r\n\r\n"))
	if idx == -1 {
		return nil, nil
	}
	m := NewMessage()
	header := bytes.Split(bb[:idx], []byte("\r\n"))
	if len(header) < 1 {
		return nil, fmt.Errorf("HTTP format: Missing first line")
	}
	meta := string(header[0])
	metaFields := strings.Fields(meta)
	if len(metaFields) < 2 {
		return nil, fmt.Errorf("HTTP format: first line invalid")
	}
	if strings.HasPrefix(strings.ToUpper(metaFields[0]), "HTTP") {
		m.Status, _ = strconv.Atoi(metaFields[1])
	} else {
		m.Method = metaFields[0]
		m.Url = metaFields[1]
	}

	for i := 1; i < len(header); i++ {
		s := string(header[i])
		kv := strings.SplitN(s, ":", 2)
		if len(kv) < 2 {
			continue //ignore
		}
		key := strings.ToLower(strings.TrimSpace(kv[0]))
		val := strings.TrimSpace(kv[1])
		m.SetHeader(key, val)
	}
	bodyLen := 0
	lenStr := m.GetHeaderNil("content-length")
	if lenStr != nil {
		bodyLen, _ = strconv.Atoi(lenStr.(string))
	}
	if (buf.Len() - idx - 4) < bodyLen {
		return nil, nil
	}
	if bodyLen > 0 {
		m.SetBody(bb[idx+4 : idx+4+bodyLen])
	}
	data := make([]byte, idx+4+bodyLen)
	buf.Read(data)
	return m, nil
}

//////////////////////////////The following are all helper Getter/Setter of Header///////////////////////////

//GetHeader key=value
func (m *Message) GetHeader(key string) string {
	val := m.Header.Get(key)
	if val == nil {
		return ""
	}
	return val.(string)
}

//GetHeaderNil key=value
func (m *Message) GetHeaderNil(key string) interface{} {
	return m.Header.Get(key)
}

//SetHeader key=value
func (m *Message) SetHeader(key string, value string) {
	if value == "" {
		return
	}
	m.Header.Set(key, value)
}

//RemoveHeader key
func (m *Message) RemoveHeader(key string) {
	m.Header.Remove(key)
}

//Cmd key=cmd
func (m *Message) Cmd() string {
	return m.GetHeader(proto.Cmd)
}

//SetCmd key=cmd
func (m *Message) SetCmd(value string) {
	m.SetHeader(proto.Cmd, value)
}

//Ack return whether ack header set or not, default to true
func (m *Message) Ack() bool {
	ack := m.GetHeader(proto.Ack)
	if ack == "" {
		return true //default to ack if not set
	}
	boolAck, err := strconv.ParseBool(ack)
	if err != nil {
		return false
	}
	return boolAck
}

//SetAck set ack value to header
func (m *Message) SetAck(ack bool) {
	m.SetHeader(proto.Ack, fmt.Sprintf("%v", ack))
}

//Id key=id
func (m *Message) Id() string {
	return m.GetHeader(proto.Id)
}

//SetId key=id
func (m *Message) SetId(value string) {
	m.SetHeader(proto.Id, value)
}

//Tag key=tag
func (m *Message) Tag() string {
	return m.GetHeader(proto.Tag)
}

//SetTag key=tag
func (m *Message) SetTag(value string) {
	m.SetHeader(proto.Tag, value)
}

//OriginId key=origin_id
func (m *Message) OriginId() string {
	return m.GetHeader(proto.OriginId)
}

//SetOriginId key=origin_id
func (m *Message) SetOriginId(value string) {
	m.SetHeader(proto.OriginId, value)
}

//Topic key=topic
func (m *Message) Topic() string {
	return m.GetHeader(proto.Topic)
}

//SetTopic key=topic
func (m *Message) SetTopic(value string) {
	m.SetHeader(proto.Topic, value)
}

//ConsumeGroup key=consume_group
func (m *Message) ConsumeGroup() string {
	return m.GetHeader(proto.ConsumeGroup)
}

//SetConsumeGroup key=consume_group
func (m *Message) SetConsumeGroup(value string) {
	m.SetHeader(proto.ConsumeGroup, value)
}

//GroupFilter key=group_filter
func (m *Message) GroupFilter() *string {
	s := m.GetHeaderNil(proto.GroupFilter)
	if s == nil {
		return nil
	}
	return &[]string{s.(string)}[0]
}

//SetGroupFilter key=group_filter
func (m *Message) SetGroupFilter(value string) {
	m.SetHeader(proto.GroupFilter, value)
}

//GroupMask key=group_mask
func (m *Message) GroupMask() *int32 {
	s := m.GetHeaderNil(proto.GroupMask)
	if s == nil {
		return nil
	}
	value, err := strconv.Atoi(s.(string))
	if err != nil {
		return nil
	}
	return &[]int32{int32(value)}[0]
}

//SetGroupMask key=group_mask
func (m *Message) SetGroupMask(value int32) {
	m.SetHeader(proto.GroupMask, strconv.Itoa(int(value)))
}

//GroupStartCopy key=group_start_copy
func (m *Message) GroupStartCopy() *string {
	s := m.GetHeaderNil(proto.GroupStartCopy)
	if s == nil {
		return nil
	}
	return &[]string{s.(string)}[0]
}

//SetGroupStartCopy key=group_start_copy
func (m *Message) SetGroupStartCopy(value string) {
	m.SetHeader(proto.GroupStartCopy, value)
}

//GroupStartOffset group_start_offset
func (m *Message) GroupStartOffset() *int64 {
	s := m.GetHeaderNil(proto.GroupStartOffset)
	if s == nil {
		return nil
	}
	value, err := strconv.ParseInt(s.(string), 10, 64)
	if err != nil {
		return nil
	}
	return &[]int64{value}[0]
}

//SetGroupStartOffset group_start_offset
func (m *Message) SetGroupStartOffset(value int64) {
	m.SetHeader(proto.GroupStartOffset, fmt.Sprintf("%d", value))
}

//GroupStartMsgid key=group_start_msgid
func (m *Message) GroupStartMsgid() *string {
	s := m.GetHeaderNil(proto.GroupStartMsgid)
	if s == nil {
		return nil
	}
	return &[]string{s.(string)}[0]
}

//SetGroupStartMsgid key=group_start_msgid
func (m *Message) SetGroupStartMsgid(value string) {
	m.SetHeader(proto.GroupStartMsgid, value)
}

//GroupStartTime group_start_time
func (m *Message) GroupStartTime() *int64 {
	s := m.GetHeaderNil(proto.GroupStartTime)
	if s == nil {
		return nil
	}
	value, err := strconv.ParseInt(s.(string), 10, 64)
	if err != nil {
		return nil
	}
	return &[]int64{value}[0]
}

//SetGroupStartTime group_start_time
func (m *Message) SetGroupStartTime(value int64) {
	m.SetHeader(proto.GroupStartTime, fmt.Sprintf("%d", value))
}

//OriginUrl key=origin_url
func (m *Message) OriginUrl() string {
	return m.GetHeader(proto.OriginUrl)
}

//SetOriginUrl key=origin_url
func (m *Message) SetOriginUrl(value string) {
	m.SetHeader(proto.OriginUrl, value)
}

//OriginStatus origin_status
func (m *Message) OriginStatus() *int {
	s := m.GetHeaderNil(proto.OriginStatus)
	if s == nil {
		return nil
	}
	value, _ := strconv.ParseInt(s.(string), 10, 32)
	return &[]int{int(value)}[0]
}

//SetOriginStatus origin_status
func (m *Message) SetOriginStatus(value int) {
	m.SetHeader(proto.OriginStatus, fmt.Sprintf("%d", value))
}

//Token key=token
func (m *Message) Token() string {
	return m.GetHeader(proto.Token)
}

//SetToken key=token
func (m *Message) SetToken(value string) {
	m.SetHeader(proto.Token, value)
}

//TopicMask key=topic_mask
func (m *Message) TopicMask() *int32 {
	s := m.GetHeaderNil(proto.TopicMask)
	if s == nil {
		return nil
	}
	value, err := strconv.Atoi(s.(string))
	if err != nil {
		return nil
	}
	return &[]int32{int32(value)}[0]
}

//SetTopicMask key=topic_mask
func (m *Message) SetTopicMask(value int32) {
	m.SetHeader(proto.TopicMask, strconv.Itoa(int(value)))
}

//Window key=window
func (m *Message) Window() *int32 {
	s := m.GetHeaderNil(proto.Window)
	if s == nil {
		return nil
	}
	value, err := strconv.Atoi(s.(string))
	if err != nil {
		return nil
	}
	return &[]int32{int32(value)}[0]
}

//SetWindow key=window
func (m *Message) SetWindow(value int32) {
	m.SetHeader(proto.Window, strconv.Itoa(int(value)))
}

//Recver key=recver
func (m *Message) Recver() string {
	return m.GetHeader(proto.Recver)
}

//SetRecver key=recver
func (m *Message) SetRecver(value string) {
	m.SetHeader(proto.Recver, value)
}
