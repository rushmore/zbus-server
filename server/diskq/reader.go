package diskq

import (
	"fmt"
	"path/filepath"
	"strings"
)

const (
	FilterPos    = 12
	FilterMaxLen = 127
)

//QueueReader to read from Queue
type QueueReader struct {
	*MappedFile
	Available chan bool

	group       string
	offset      int32
	msgNo       int64
	filter      string
	filterParts []string

	index   *Index
	blockNo int64
	block   *Block
}

//NewQueueReader create reader
func NewQueueReader(index *Index, group string) (*QueueReader, error) {
	return newQueueReader(index, nil, group)
}

//NewQueueReaderCopy copy from another reader
func NewQueueReaderCopy(copy *QueueReader, group string) (*QueueReader, error) {
	return newQueueReader(copy.index, copy, group)
}

func newQueueReader(index *Index, copy *QueueReader, group string) (*QueueReader, error) {
	fullPath := filepath.Join(index.dirPath, ReaderDir, fmt.Sprintf("%s%s", group, ReaderSuffix))
	m, err := NewMappedFile(fullPath, ReaderSize)
	if err != nil {
		return nil, err
	}
	r := &QueueReader{}
	r.MappedFile = m
	r.group = group
	r.index = index
	r.Available = make(chan bool)

	if copy == nil {
		if r.newFile {
			r.blockNo = index.BlockStart()
			r.offset = 0
			r.writeOffset()
			r.buf.SetPos(FilterPos)
			r.buf.PutString(r.filter)
		} else {
			r.buf.SetPos(0)
			r.blockNo, _ = r.buf.GetInt64()
			r.offset, _ = r.buf.GetInt32()
			r.filter, _ = r.buf.GetStringN(FilterMaxLen + 1)
			r.filterParts = strings.Split(r.filter, ".")
		}
	} else {
		r.blockNo = copy.blockNo
		r.msgNo = copy.msgNo
		r.writeOffset()
		r.buf.SetPos(FilterPos)
		r.buf.PutString(r.filter)
	}

	start := r.index.BlockStart()
	if r.blockNo < start { //if block number invalid
		r.blockNo = start
		r.offset = 0
		r.writeOffset()
	}
	if r.index.IsOverflow(r.blockNo) {
		r.blockNo = r.index.CurrBlockNo()
		r.offset = 0
		r.writeOffset()
	}

	r.block, err = r.index.LoadBlock(r.blockNo)
	if err != nil {
		r.Close()
		return nil, err
	}
	err = r.readMsgNo()
	if err != nil {
		r.Close()
		return nil, err
	}

	return r, nil
}

//Close destroy the queue reader
func (r *QueueReader) Close() {
	r.MappedFile.Close()
	if r.block != nil {
		r.block.Close()
	}
}

//BlockNo returns block number
func (r *QueueReader) BlockNo() int64 {
	return r.blockNo
}

//MsgNo returns message number
func (r *QueueReader) MsgNo() int64 {
	return r.msgNo
}

//MsgCount remaining message count
func (r *QueueReader) MsgCount() int64 {
	c := r.index.MsgNo() - r.msgNo - 1
	if c < 0 {
		c = 0 //initialized index
	}
	return c
}

//Name returns reader/group name
func (r *QueueReader) Name() string {
	return r.group
}

//Filter returns filter
func (r *QueueReader) Filter() string {
	return r.filter
}

func (r *QueueReader) Read() (*DiskMsg, error) {
	r.mutex.Lock()
	defer r.mutex.Unlock()

	return r.read()
}

func (r *QueueReader) read() (*DiskMsg, error) {
	if r.block.IsBlockEnd(int(r.offset)) {
		if r.index.IsOverflow(r.blockNo + 1) {
			return nil, nil
		}
		r.blockNo++
		r.block.Close()
		var err error
		r.block, err = r.index.LoadBlock(r.blockNo)
		if err != nil {
			return nil, err
		}
		r.offset = 0
		r.writeOffset()
	}
	m, msgNo, bytesRead, err := r.block.read(int(r.offset), r.filterParts)
	if err != nil {
		return nil, err
	}
	r.offset += int32(bytesRead)
	if msgNo >= 0 {
		r.msgNo = msgNo
	}
	r.writeOffset()
	if m == nil {
		return r.read()
	}
	return m, nil
}

//SetFilter of reader
func (r *QueueReader) SetFilter(value string) {
	r.mutex.Lock()
	defer r.mutex.Unlock()

	r.filter = value
	r.filterParts = strings.Split(r.filter, ".")
	r.buf.SetPos(FilterPos)
	r.buf.PutStringN(r.filter, FilterMaxLen+1)
}

func (r *QueueReader) writeOffset() {
	r.buf.SetPos(0)
	r.buf.PutInt64(r.blockNo)
	r.buf.PutInt32(r.offset)
}

func (r *QueueReader) readMsgNo() error {
	if r.block.IsBlockEnd(int(r.offset)) { //just at the end of block
		if r.index.IsOverflow(r.blockNo + 1) { //no more block available
			r.msgNo = r.index.MsgNo() - 1
			return nil
		}
		r.blockNo++ //forward to next readable block
		r.block.Close()
		var err error
		r.block, err = r.index.LoadBlock(r.blockNo)
		if err != nil {
			return err
		}
		r.offset = 0
		r.writeOffset()
	}
	msg, _, _, err := r.block.Read(int(r.offset), nil)
	r.msgNo = msg.MsgNo - 1
	return err
}
