package diskq

import (
	"sync"
)

//QueueWriter to write into Queue
type QueueWriter struct {
	index *Index
	block *Block
	mutex *sync.Mutex
}

//NewQueueWriter create writer
func NewQueueWriter(index *Index) (*QueueWriter, error) {
	w := &QueueWriter{index, nil, &sync.Mutex{}}
	var err error
	w.block, err = index.LoadBlockToWrite()
	if err != nil {
		return nil, err
	}
	return w, nil
}

//Close the writer
func (w *QueueWriter) Close() {
	if w.block != nil {
		w.block.Close()
		w.block = nil
	}
}

func (w *QueueWriter) Write(m *DiskMsg) (int, error) {
	w.mutex.Lock()
	defer w.mutex.Unlock()
	count, err := w.block.Write(m)

	if count <= 0 && err == nil {
		w.block.Close()
		w.block, _ = w.index.LoadBlockToWrite()
		return w.block.Write(m)
	}

	return count, err
}

//WriteBatch write multiple msg in one batch
func (w *QueueWriter) WriteBatch(msgs []*DiskMsg) (int, error) {
	w.mutex.Lock()
	defer w.mutex.Unlock()

	count, err := w.block.WriteBatch(msgs)

	if count <= 0 && err == nil {
		w.block.Close()
		w.block, _ = w.index.LoadBlockToWrite()
		return w.block.WriteBatch(msgs)
	}

	return count, err
}
