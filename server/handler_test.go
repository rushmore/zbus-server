package main

import (
	"testing"
)

func Test_handleUrlMessage(t *testing.T) {
	msg := NewMessage()
	msg.Url = "/rpc/MyRpc/plus/1/2/?key1=val1&&key2=value2"
	handleUrlMessage(msg)

}

func Test_bindata(t *testing.T) {
	data, err := Asset("asset/base.css")
	if err != nil {
		t.Fail()
	}
	print(string(data))
}
