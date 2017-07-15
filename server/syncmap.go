package main

import (
	"sync"
)

//SyncMap goroutine safe map
//ReadWirte mutex applied, only support string type key
type SyncMap struct {
	Map map[string]interface{}
	sync.RWMutex
}

//Get by key
func (m *SyncMap) Get(key string) interface{} {
	m.RLock()
	defer m.RUnlock()
	val, ok := m.Map[key]
	if !ok {
		return nil
	}
	return val
}

//Set key-value pair
func (m *SyncMap) Set(key string, val interface{}) {
	m.Lock()
	defer m.Unlock()
	m.Map[key] = val
}

//Remove key
func (m *SyncMap) Remove(key string) interface{} {
	m.Lock()
	defer m.Unlock()
	val := m.Map[key]
	delete(m.Map, key)
	return val
}

//Copy as map[string]string
func (m *SyncMap) Copy() map[string]string {
	m.RLock()
	defer m.RUnlock()

	res := make(map[string]string)
	for key, val := range m.Map {
		value, ok := val.(string)
		if ok {
			res[key] = value
		}
	}
	return res
}

//Contains check key exists
func (m *SyncMap) Contains(key string) bool {
	m.RLock()
	defer m.RUnlock()
	_, ok := m.Map[key]
	return ok
}

//Size return number of keys in map
func (m *SyncMap) Size() int {
	m.RLock()
	defer m.RUnlock()
	return len(m.Map)
}

//Clear all key-values
func (m *SyncMap) Clear() {
	m.Lock()
	defer m.Unlock()
	m.Map = make(map[string]interface{})
}
