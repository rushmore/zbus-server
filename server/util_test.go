package main

import (
	"testing"
)

func TestLocalIpAddress(t *testing.T) {
	ip, err := LocalIPAddress()
	if err != nil {
		t.Fail()
	}
	println(ip.String())
}
