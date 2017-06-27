package diskq

import (
	"fmt"
	"os"
	"strings"
	"sync"
	"time"
)

const (
	MsgIdMaxLen  = 39
	MsgTagMaxLen = 127
	MsgBodyPos   = 8 + 8 + 40 + 8 + 8 + 128 //200
)

//DiskMsg to read and write in disk for DiskQ
type DiskMsg struct {
	Offset     int64
	Timestamp  int64
	Id         string // write 40 = 1_len + max 39
	CorrOffset int64
	MsgNo      int64
	Tag        string // write 128 = 1_len + max 127
	Body       []byte // write 4_len + body
}

//Block is a read/write block file
type Block struct {
	index   *Index
	blockNo int64
	file    *os.File
	mutex   *sync.Mutex
}

//NewBlock create block of an Index
func newBlock(index *Index, blockNo int64, file *os.File) *Block {
	b := &Block{}
	b.index = index
	b.blockNo = blockNo
	b.file = file
	b.mutex = &sync.Mutex{}
	return b
}

//Close the block file
func (b *Block) Close() {
	b.file.Close()
}

//IsFull test if the block is full
func (b *Block) IsFull() bool {
	return b.index.ReadOffset(b.blockNo).EndOffset >= BlockMaxSize
}

//IsBlockEnd test if the pos passed the block end
func (b *Block) IsBlockEnd(pos int) bool {
	return int32(pos) >= b.index.ReadOffset(b.blockNo).EndOffset
}

//Write message to disk
func (b *Block) Write(msg *DiskMsg) (int, error) {
	b.mutex.Lock()
	defer b.mutex.Unlock()

	startOffset := b.index.ReadOffset(b.blockNo).EndOffset
	if startOffset >= BlockMaxSize {
		return 0, nil
	}

	msg.Offset = int64(startOffset)
	msg.MsgNo = b.index.GetAndAddMsgNo(1)
	msgSize := msg.Size()
	buf := NewFixedBuf(msgSize)
	msg.writeToBuffer(buf)
	b.file.Seek(int64(startOffset), 0)
	n, err := b.file.Write(buf.Bytes())
	if err != nil {
		return n, err
	}
	b.index.WriteEndOffset(startOffset + int32(msgSize))
	return n, err
}

//WriteBatch write message in batch mode
func (b *Block) WriteBatch(msgs []*DiskMsg) (int, error) {
	if len(msgs) == 0 {
		return 0, fmt.Errorf("WriteBatch msgs parameter length should >= 1")
	}

	b.mutex.Lock()
	defer b.mutex.Unlock()

	startOffset := b.index.ReadOffset(b.blockNo).EndOffset
	if startOffset >= BlockMaxSize {
		return 0, fmt.Errorf("Block full")
	}
	totalSize := 0
	offset := startOffset
	msgNo := b.index.GetAndAddMsgNo(len(msgs))
	for _, msg := range msgs {
		msg.Offset = int64(offset)
		msg.MsgNo = msgNo
		msgNo++
		msgSize := msg.Size()
		totalSize += msgSize
		offset += int32(msgSize)
	}
	buf := NewFixedBuf(totalSize)
	for _, msg := range msgs {
		msg.writeToBuffer(buf)
	}

	b.file.Seek(int64(startOffset), 0)
	n, err := b.file.Write(buf.Bytes())
	if err != nil {
		return n, err
	}
	b.index.WriteEndOffset(startOffset + int32(totalSize))
	return n, err
}

//Size return the size of the message in bytes
func (m *DiskMsg) Size() int {
	bodySize := len(m.Body)
	return 4 + bodySize + MsgBodyPos
}

func (m *DiskMsg) writeToBuffer(buf *FixedBuf) error {
	if m.Size() > buf.Remaining() {
		return fmt.Errorf("buffer size not enough")
	}
	buf.PutInt64(m.Offset)
	if m.Timestamp <= 0 {
		buf.PutInt64(time.Now().UnixNano() / int64(time.Millisecond))
	} else {
		buf.PutInt64(m.Timestamp)
	}
	buf.PutStringN(m.Id, MsgIdMaxLen+1)
	if m.CorrOffset <= 0 {
		buf.PutInt64(0)
	} else {
		buf.PutInt64(m.CorrOffset)
	}
	buf.PutInt64(m.MsgNo)
	buf.PutStringN(m.Tag, MsgTagMaxLen+1)

	buf.PutInt32(int32(len(m.Body)))
	if m.Body != nil {
		buf.PutBytes(m.Body)
	}
	return nil
}

//Read message at pos, filterPart could be nil -- no filter on message
func (b *Block) Read(pos int, filterParts []string) (*DiskMsg, int64, int, error) {
	b.mutex.Lock()
	defer b.mutex.Unlock()

	return b.read(pos, filterParts)
}

func filterMatch(filterParts []string, tag string) bool {
	if filterParts == nil {
		return true
	}
	tagParts := strings.Split(tag, ".")
	for i := 0; i < len(filterParts); i++ {
		filterPart := filterParts[i]
		if i >= len(tagParts) {
			return false
		}
		tagPart := tagParts[i]
		if filterPart == "+" {
			continue
		}
		if filterPart == "*" {
			return true
		}
		if filterPart == tagPart {
			continue
		}
		return false
	}
	return len(filterParts) == len(tagParts)
}

//return last read message number(lastMsgNo), bytesRead, nil if no error found
func (b *Block) read(pos int, filterParts []string) (*DiskMsg, int64, int, error) {
	bytesRead := 0
	lastMsgNo := int64(-1)
	for {
		if b.IsBlockEnd(pos + bytesRead) {
			break
		}
		msg, bodySize, err := b.readHead(pos + bytesRead)
		if err != nil {
			return nil, lastMsgNo, bytesRead, err
		}
		lastMsgNo = msg.MsgNo
		bytesRead += int(bodySize + 4 + MsgBodyPos)
		if !filterMatch(filterParts, msg.Tag) {
			continue
		}
		msg.Body = make([]byte, bodySize)
		_, err = b.file.Read(msg.Body)
		if err != nil {
			return nil, lastMsgNo, bytesRead, err
		}
		return msg, lastMsgNo, bytesRead, nil
	}
	return nil, lastMsgNo, bytesRead, nil
}

func (b *Block) readHead(pos int) (*DiskMsg, int32, error) {
	_, err := b.file.Seek(int64(pos), 0)
	if err != nil {
		return nil, 0, err
	}
	head := make([]byte, MsgBodyPos+4) //read length
	_, err = b.file.Read(head)
	if err != nil {
		return nil, 0, err
	}
	buf := NewFixedBufFWrap(head)
	m := &DiskMsg{}
	m.Offset, _ = buf.GetInt64()
	m.Timestamp, _ = buf.GetInt64()
	m.Id, _ = buf.GetStringN(MsgIdMaxLen + 1)
	m.CorrOffset, _ = buf.GetInt64()
	m.MsgNo, _ = buf.GetInt64()
	m.Tag, _ = buf.GetStringN(MsgTagMaxLen + 1)

	bodySize, _ := buf.GetInt32()
	if bodySize < 0 {
		return nil, 0, fmt.Errorf("Block.read bodySize < 0")
	}
	return m, bodySize, nil
}
