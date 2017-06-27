package main

import (
	"crypto/rand"
	"fmt"
	"net"
	"os"
	"sort"
	"strings"
	"sync"
	"time"
)

//SyncMap safe map
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

//Clear all key-values
func (m *SyncMap) Clear() {
	m.Lock()
	defer m.Unlock()
	m.Map = make(map[string]interface{})
}

//UUID generate psudo uuid string
func uuid() string {
	b := make([]byte, 16)
	rand.Read(b)
	return fmt.Sprintf("%x-%x-%x-%x-%x", b[0:4], b[4:6], b[6:8], b[8:10], b[10:])
}

//CurrMillis returns current milliseconds of Unix time
func CurrMillis() int64 {
	return time.Now().UnixNano() / int64(time.Millisecond)
}

var fileMap map[string][]byte

//ReadAssetFile read asset file via go binary data or direct io
func ReadAssetFile(file string) ([]byte, error) {
	//return ioutil.ReadFile(fmt.Sprintf("asset/%s", file))

	if fileMap == nil {
		fileMap = make(map[string][]byte)
	}
	fileData, ok := fileMap[file]
	if !ok {
		fileData, err := Asset(fmt.Sprintf("asset/%s", file))
		if err == nil {
			fileMap[file] = fileData
		}
		return fileData, err
	}
	return fileData, nil
}

//SplitClean splits string without empty
func SplitClean(s string, sep string) []string {
	if s == "" {
		return []string{}
	}
	bb := strings.Split(s, sep)
	var r []string
	for _, str := range bb {
		if str != "" {
			r = append(r, strings.TrimSpace(str))
		}
	}
	return r
}

//ServerAddress find the real address
func ServerAddress(addr string) string {
	bb := SplitClean(addr, ":")
	host := bb[0]
	port := "80"
	if len(bb) > 1 {
		port = bb[1]
	}
	if host == "0.0.0.0" || host == "" {
		ip, err := LocalIPAddress()
		if err == nil {
			host = ip.String()
		}
	}
	return fmt.Sprintf("%s:%s", host, port)
}

type byIP []net.IP

func (s byIP) Len() int {
	return len(s)
}
func (s byIP) Swap(i, j int) {
	s[i], s[j] = s[j], s[i]
}

func rank(ip net.IP) int {
	prefix := []string{"10.", "172.", "192.", "127."}
	str := ip.String()
	for i := 0; i < len(prefix); i++ {
		if strings.HasPrefix(str, prefix[i]) {
			return i
		}
	}
	return 0
}

func (s byIP) Less(i, j int) bool {
	return rank(s[i]) < rank(s[j])
}

//LocalIPAddress get local IP by preference PublicIP > 10.*> 172.* > 192.* > 127.*
func LocalIPAddress() (net.IP, error) {
	ifaces, err := net.Interfaces()
	if err != nil {
		return nil, err
	}
	addresses := []net.IP{}
	for _, iface := range ifaces {
		if iface.Flags&net.FlagUp == 0 {
			continue // interface down
		}
		addrs, err := iface.Addrs()
		if err != nil {
			continue
		}

		for _, addr := range addrs {
			var ip net.IP
			switch v := addr.(type) {
			case *net.IPNet:
				ip = v.IP
			case *net.IPAddr:
				ip = v.IP
			}
			if ip == nil {
				continue
			}
			ip = ip.To4()
			if ip == nil {
				continue // not an ipv4 address
			}
			addresses = append(addresses, ip)
		}
	}
	if len(addresses) == 0 {
		return nil, fmt.Errorf("no address Found, net.InterfaceAddrs: %v", addresses)
	}
	sort.Sort(byIP(addresses))
	return addresses[0], nil
}

//EnsureDir create dir if not exists
func EnsureDir(dir string) error {
	if _, err := os.Stat(dir); err != nil {
		if err := os.MkdirAll(dir, 0777); err != nil {
			return err
		}
	}
	return nil
}
