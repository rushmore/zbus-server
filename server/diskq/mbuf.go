package diskq

import (
	"encoding/binary"
	"fmt"
	"os"

	"path/filepath"

	"./mmap"
)

//FixedBuf is a fixed length of buffer
type FixedBuf struct {
	data []byte
	pos  int
	cap  int
	bo   binary.ByteOrder
}

//NewFixedBuf create fixed length buffer
func NewFixedBuf(cap int) *FixedBuf {
	data := make([]byte, cap)
	return &FixedBuf{data, 0, cap, binary.BigEndian}
}

//NewFixedBufFWrap create fixed lenght buffer from data
func NewFixedBufFWrap(data []byte) *FixedBuf {
	n := len(data)
	return &FixedBuf{data, 0, n, binary.BigEndian}
}

//Bytes return available data
func (b *FixedBuf) Bytes() []byte {
	return b.data[0:b.pos]
}

//Cap return capacity of the buffer
func (b *FixedBuf) Cap() int {
	return b.cap
}

//Remaining return remaing byte count of the buffer
func (b *FixedBuf) Remaining() int {
	return b.cap - b.pos
}

func (b *FixedBuf) check(forward int) error {
	if b.pos+forward > b.cap {
		return fmt.Errorf("pos=%d, invalid to forward %d byte", b.pos, forward)
	}
	return nil
}

//SetPos set position
func (b *FixedBuf) SetPos(pos int) {
	b.pos = pos
}

//GetByte read one byte
func (b *FixedBuf) GetByte() (byte, error) {
	if err := b.check(1); err != nil {
		return 0, err
	}
	value := b.data[b.pos]
	b.pos++
	return value, nil
}

//GetInt16 read two bytes as int16
func (b *FixedBuf) GetInt16() (int16, error) {
	n := 2
	if err := b.check(n); err != nil {
		return 0, err
	}
	value := b.bo.Uint16(b.data[b.pos : b.pos+n])
	b.pos += n
	return int16(value), nil
}

//GetInt32 read two bytes as int32
func (b *FixedBuf) GetInt32() (int32, error) {
	n := 4
	if err := b.check(n); err != nil {
		return 0, err
	}
	value := b.bo.Uint32(b.data[b.pos : b.pos+n])
	b.pos += n
	return int32(value), nil
}

//GetInt64 read two bytes as int64
func (b *FixedBuf) GetInt64() (int64, error) {
	n := 8
	if err := b.check(n); err != nil {
		return 0, err
	}
	value := b.bo.Uint64(b.data[b.pos : b.pos+n])
	b.pos += n
	return int64(value), nil
}

//GetBytes read n bytes
func (b *FixedBuf) GetBytes(n int) ([]byte, error) {
	if err := b.check(n); err != nil {
		return nil, err
	}
	value := b.data[b.pos : b.pos+n]
	b.pos += n
	return value, nil
}

//GetString read string(1_len + max 127 string)
func (b *FixedBuf) GetString() (string, error) {
	if err := b.check(1); err != nil {
		return "", err
	}
	n := int(b.data[b.pos])
	if n < 0 || n > 127 {
		return "", fmt.Errorf("GetString error, invalid length in first byte")
	}
	if err := b.check(n + 1); err != nil {
		return "", err
	}

	value := string(b.data[b.pos+1 : b.pos+n+1])
	b.pos += n + 1
	return value, nil
}

//GetStringN read string forwarding spanSize
func (b *FixedBuf) GetStringN(spanSize int) (string, error) {
	if err := b.check(spanSize); err != nil {
		return "", err
	}
	n := int(b.data[b.pos])
	if n < 0 || n >= spanSize {
		return "", fmt.Errorf("GetString error, invalid length in first byte")
	}
	value := string(b.data[b.pos+1 : b.pos+n+1])
	b.pos += spanSize
	return value, nil
}

//PutString write string(1_len + max 127 string)
func (b *FixedBuf) PutString(value string) error {
	n := len(value)
	if n > 127 {
		return fmt.Errorf("PutString error, string longer than 127")
	}
	if err := b.check(n + 1); err != nil {
		return err
	}
	b.data[b.pos] = byte(n)
	copy(b.data[b.pos+1:], []byte(value))
	b.pos += (n + 1)
	return nil
}

//PutStringN write string(1_len + max 127 string)
func (b *FixedBuf) PutStringN(value string, spanSize int) error {
	if err := b.check(spanSize); err != nil {
		return err
	}
	n := len(value)
	if n >= spanSize {
		return fmt.Errorf("PutString error, string longer than %d", spanSize-1)
	}

	b.data[b.pos] = byte(n)
	copy(b.data[b.pos+1:], []byte(value))
	b.pos += spanSize
	return nil
}

//PutByte write one byte
func (b *FixedBuf) PutByte(value byte) error {
	n := 1
	if err := b.check(n); err != nil {
		return err
	}
	b.data[b.pos] = value
	b.pos += n
	return nil
}

//PutInt16 write int16
func (b *FixedBuf) PutInt16(value int16) error {
	n := 2
	if err := b.check(n); err != nil {
		return err
	}
	b.bo.PutUint16(b.data[b.pos:b.pos+n], uint16(value))
	b.pos += n
	return nil
}

//PutInt32 write int32
func (b *FixedBuf) PutInt32(value int32) error {
	n := 4
	if err := b.check(n); err != nil {
		return err
	}
	b.bo.PutUint32(b.data[b.pos:b.pos+n], uint32(value))
	b.pos += n
	return nil
}

//PutInt64 write int64
func (b *FixedBuf) PutInt64(value int64) error {
	n := 8
	if err := b.check(n); err != nil {
		return err
	}
	b.bo.PutUint64(b.data[b.pos:b.pos+n], uint64(value))
	b.pos += n
	return nil
}

//PutBytes write n bytes
func (b *FixedBuf) PutBytes(value []byte) error {
	n := len(value)
	if err := b.check(n); err != nil {
		return err
	}
	copy(b.data[b.pos:], value)
	b.pos += n
	return nil
}

//MappedBuf is a memory mapped file buffer, read and writes to file like in memory
type MappedBuf struct {
	*FixedBuf
	mdata mmap.MMap

	file *os.File
}

//NewMappedBuf MappedBuf from file
func NewMappedBuf(fileName string, length int) (*MappedBuf, error) {
	dir := filepath.Dir(fileName)
	if err := os.MkdirAll(dir, 0777); err != nil {
		return nil, err
	}
	file, err := os.OpenFile(fileName, os.O_RDWR|os.O_CREATE, 0777)
	if err != nil {
		return nil, err
	}
	fi, err := file.Stat()
	if err != nil {
		file.Close()
		return nil, err
	}
	fsize := fi.Size()
	nLen := int64(length)
	if fsize > nLen {
		file.Truncate(nLen)
	}
	if fsize < nLen {
		zeros := make([]byte, nLen-fsize)
		if _, err := file.WriteAt(zeros, fsize); err != nil {
			return nil, err
		}
	}
	data, err := mmap.MapRegion(file, length, mmap.RDWR, 0, 0)
	if err != nil {
		return nil, err
	}
	buf := NewFixedBufFWrap(data)
	return &MappedBuf{buf, data, file}, nil
}

//Close the MappedByteBuffer
func (b *MappedBuf) Close() error {
	err := b.mdata.Unmap()
	err2 := b.file.Close()
	if err != nil {
		return err
	}
	if err2 != nil {
		return err2
	}
	return nil
}
