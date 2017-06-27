package diskq

import (
	"fmt"
	"testing"
)

var qwriter, _ = NewQueueWriter(index)

func TestNewQueueWriter(t *testing.T) {

}

func TestQueueWriter_Write(t *testing.T) {
	if index == nil {
		t.Fail()
	}
	msg := &DiskMsg{}
	msg.Body = []byte("hello world")
	qwriter.Write(msg)
}

func BenchmarkQueueWriter_Write(b *testing.B) {
	if index == nil {
		b.Fail()
	}
	for i := 0; i < b.N; i++ {
		msg := &DiskMsg{}
		if i%100 == 0 {
			msg.Tag = fmt.Sprintf("abc.%d", i)
		}
		msg.Body = make([]byte, 102400)
		qwriter.Write(msg)
	}
}
