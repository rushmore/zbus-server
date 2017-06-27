package diskq

import (
	"fmt"
	"strings"
	"testing"
)

func newIndex() *Index {
	idx, err := NewIndex("/tmp/IndexExample")
	if err != nil {
		return nil
	}
	return idx
}

var index = newIndex()

func TestNewQueueReader(t *testing.T) {
	qreader, err := NewQueueReader(index, "MyGroup")
	if err != nil {
		t.Fail()
	}
	fmt.Println(qreader)
	qreader.Close()
}

func TestFilterSplit(t *testing.T) {
	filter := "abc.def.#.dd.*"
	bb := strings.Split(filter, ".")
	println(len(bb))
	for _, b := range bb {
		println(b)
	}
}

func TestQueueReader_SetFilter(t *testing.T) {
	qreader, err := NewQueueReader(index, "MyGroup")
	if err != nil {
		t.Fail()
	}
	qreader.SetFilter("abc.*")
	qreader.Close()

	qreader, err = NewQueueReader(index, "MyGroup")
	if err != nil {
		t.Fail()
	}
	if qreader.Filter() != "abc.*" {
		t.Fail()
	}
}

func TestQueueReader_Read(t *testing.T) {
	r, err := NewQueueReader(index, "MyGroup")
	if err != nil {
		t.Fail()
	}
	defer r.Close()

	r.SetFilter("abc.*")
	for {
		m, err := r.Read()
		if err != nil || m == nil {
			break
		}
		println(m.Offset, m.Tag)
	}
}
