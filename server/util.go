package main

import (
	"crypto/rand"
	"fmt"
	"net"
	"os"
	"sort"
	"strconv"
	"strings"
	"time"

	"./proto"
)

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
func ServerAddress(addr string) (string, int) {
	bb := strings.Split(addr, ":")
	host := bb[0]
	port := 80
	if len(bb) > 1 {
		port, _ = strconv.Atoi(bb[1])
	}
	if host == "0.0.0.0" || host == "" {
		ip, err := LocalIPAddress()
		if err == nil {
			host = ip.String()
		}
	}
	return host, port
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

//ParseServerAddressList parse server address list
func ParseServerAddressList(addr string) []*proto.ServerAddress {
	addrList := SplitClean(addr, ";")
	var res []*proto.ServerAddress
	for _, addr := range addrList {
		if addr == "" {
			continue
		}
		serverAddr := &proto.ServerAddress{}
		if strings.HasPrefix(strings.ToUpper(addr), "[SSL]") {
			addr = addr[5:len(addr)]
			serverAddr.Address = addr
			serverAddr.SslEnabled = true
		} else {
			serverAddr.Address = addr
			serverAddr.SslEnabled = false
		}
		res = append(res, serverAddr)
	}
	if res == nil {
		return []*proto.ServerAddress{}
	}

	return res
}
