package main

import (
	"strings"
	"testing"
)

func TestCodecMessage(t *testing.T) {
	s := "xxx xx"
	bb := strings.Fields(s)
	println(len(bb))
}
