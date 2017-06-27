package diskq

import (
	"testing"
)

func TestNewIndex(t *testing.T) {
	idx, err := NewIndex("/tmp/IndexExample")
	if err != nil {
		t.Fail()
	}
	defer idx.Close()
	if idx.version != IndexVersion {
		t.Fail()
	}
}

func TestIndex_LoadBlockToWrite(t *testing.T) {
	idx, err := NewIndex("/tmp/IndexExample")
	if err != nil {
		t.Fail()
	}
	defer idx.Close()

	block, err := idx.LoadBlockToWrite()
	if err != nil {
		t.Fail()
	}
	block.Close()
}
