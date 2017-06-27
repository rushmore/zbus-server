package proto

import (
	"fmt"
)

const (
	VersionValue = "0.8.0"
	//MQ Produce/Consume
	Produce = "produce"
	Consume = "consume"
	Rpc     = "rpc"
	Route   = "route" //route back message to sender, designed for RPC

	//Topic/ConsumeGroup control
	Declare = "declare"
	Query   = "query"
	Remove  = "remove"
	Empty   = "empty"

	//Tracker
	TrackPub = "track_pub"
	TrackSub = "track_sub"
	Tracker  = "tracker"
	Server   = "server"

	Cmd       = "cmd"
	Topic     = "topic"
	TopicMask = "topic_mask"
	Tag       = "tag"
	Offset    = "offset"

	ConsumeGroup     = "consume_group"
	GroupStartCopy   = "group_start_copy"
	GroupStartOffset = "group_start_offset"
	GroupStartMsgid  = "group_start_msgid"
	GroupStartTime   = "group_start_time"
	GroupFilter      = "group_filter"
	GroupMask        = "group_mask"
	ConsumeWindow    = "consume_window"

	Sender = "sender"
	Recver = "recver"
	Id     = "id"
	Host   = "host"

	Ack      = "ack"
	Encoding = "encoding"

	OriginId     = "origin_id"
	OriginUrl    = "origin_url"
	OriginStatus = "origin_status"

	//Security

	Token = "token"

	MaskPause        = 1 << 0
	MaskRpc          = 1 << 1
	MaskExclusive    = 1 << 2
	MaskDeleteOnExit = 1 << 3

	//Pages

	Home = ""
	Js   = "js"
	Css  = "css"
	Img  = "img"
	Page = "page"

	Heartbeat = "heartbeat"
)

//ServerAddress server address
type ServerAddress struct {
	Address    string `json:"address"`
	SslEnabled bool   `json:"sslEnabled"`
}

func (sa *ServerAddress) String() string {
	if sa.SslEnabled {
		return fmt.Sprintf("[SSL]%s", sa.Address)
	}
	return sa.Address
}

//ErrInfo for batch operation
type ErrInfo struct {
	Error error `json:"error"`
}

//TrackItem tracked item info
type TrackItem struct {
	ErrInfo       `json:""`
	ServerAddress *ServerAddress `json:"serverAddress"`
	ServerVersion string         `json:"serverVersion"`
}

//TrackerInfo tracker info
type TrackerInfo struct {
	TrackItem   `json:""`
	InfoVersion int64                  `json:"infoVersion"`
	ServerTable map[string]*ServerInfo `json:"serverTable"`
}

//ServerInfo server info
type ServerInfo struct {
	TrackItem   `json:""`
	InfoVersion int64                 `json:"infoVersion"`
	TrackerList []ServerAddress       `json:"trackerList"`
	TopicTable  map[string]*TopicInfo `json:"topicTable"`
}

//TopicInfo topic info
type TopicInfo struct {
	TrackItem        `json:""`
	TopicName        string              `json:"topicName"`
	Mask             int32               `json:"mask"`
	MessageDepth     int64               `json:"messageDepth"`  //message count on disk
	ConsumerCount    int32               `json:"consumerCount"` //total consumer count in consumeGroupList
	ConsumeGroupList []*ConsumeGroupInfo `json:"consumeGroupList"`
	Creator          string              `json:"creator"`
	CreatedTime      int64               `json:"createdTime"`
	LastUpdatedTime  int64               `json:"lastUpdatedTime"`
}

//ConsumeGroupInfo consume group
type ConsumeGroupInfo struct {
	ErrInfo         `json:""`
	TopicName       string   `json:"topicName"`
	GroupName       string   `json:"groupName"`
	Mask            int32    `json:"mask"`
	Filter          string   `json:"filter"`
	MessageCount    int64    `json:"messageCount"`
	ConsumerCount   int32    `json:"consumerCount"`
	ConsumerList    []string `json:"consumerList"`
	Creator         string   `json:"creator"`
	CreatedTime     int64    `json:"createdTime"`
	LastUpdatedTime int64    `json:"lastUpdatedTime"`
}

//ServerEvent updates to tracker
type ServerEvent struct {
	ServerInfo *ServerInfo `json:"serverInfo"`
	Live       bool        `json:"live"`
}
