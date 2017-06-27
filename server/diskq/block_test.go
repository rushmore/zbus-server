package diskq

import (
	"fmt"
	"testing"
)

func loadBlock() *Block {
	idx, err := NewIndex("/tmp/IndexExample")
	if err != nil {
		return nil
	}
	block, err := idx.LoadBlockToWrite()
	if err != nil {
		return nil
	}
	return block
}

var block = loadBlock()

func TestDiskMsg_writeToBuffer(t *testing.T) {
	m := &DiskMsg{}
	m.Body = []byte("hello world")

	buf := NewFixedBuf(m.Size())
	m.writeToBuffer(buf)

	if buf.pos != m.Size() {
		t.Fail()
	}

	if err := buf.PutByte(byte(1)); err == nil {
		t.Fail()
	}
}

func TestBlock_Write(t *testing.T) {
	msg := &DiskMsg{}
	msg.Body = []byte("hello world")
	block.Write(msg)
}

func Benchmark_BlockWrite(b *testing.B) {
	for n := 0; n < b.N; n++ {
		msg := &DiskMsg{}
		msg.Body = []byte("hello world")
		block.Write(msg)
	}
}

func TestBlock_WriteBatch(t *testing.T) {
	msgs := make([]*DiskMsg, 10)
	for i := 0; i < len(msgs); i++ {
		msg := &DiskMsg{}
		msg.Body = []byte("hello world")
		msgs[i] = msg
	}
	block.WriteBatch(msgs)
}

func TestBlock_Read(t *testing.T) {
	n := 10
	for i := 0; i < n; i++ {
		msg := &DiskMsg{}
		msg.Body = []byte("hello world")
		block.Write(msg)
	}

	start := 0
	for i := 0; i < n; i++ {
		m, _, _, err := block.Read(start, nil)
		if err != nil {
			t.Fail()
		}
		start += m.Size()
		fmt.Println(m.Offset)
	}
}
