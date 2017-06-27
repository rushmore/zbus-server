package main

import (
	"bytes"
	"crypto/rand"
	"crypto/sha1"
	"encoding/base64"
	"io"
	"net"
	"net/url"
	"strings"
	"time"

	"./websocket"
)

// HandshakeError describes an error with the handshake from the peer.
type HandshakeError struct {
	message string
}

func (e HandshakeError) Error() string { return e.message }

// Upgrader specifies parameters for upgrading an HTTP connection to a
// WebSocket connection.
type Upgrader struct {
	// HandshakeTimeout specifies the duration for the handshake to complete.
	HandshakeTimeout time.Duration

	// ReadBufferSize and WriteBufferSize specify I/O buffer sizes. If a buffer
	// size is zero, then buffers allocated by the HTTP server are used. The
	// I/O buffer sizes do not limit the size of the messages that can be sent
	// or received.
	ReadBufferSize, WriteBufferSize int

	// Subprotocols specifies the server's supported protocols in order of
	// preference. If this field is set, then the Upgrade method negotiates a
	// subprotocol by selecting the first match in this list with a protocol
	// requested by the client.
	Subprotocols []string

	// CheckOrigin returns true if the request Origin header is acceptable. If
	// CheckOrigin is nil, the host in the Origin header must not be set or
	// must match the host of the request.
	CheckOrigin func(header map[string]string) bool
}

// Subprotocols returns the subprotocols requested by the client in the
// Sec-Websocket-Protocol header.
func Subprotocols(header map[string]string) []string {
	h := strings.TrimSpace(header["sec-websocket-protocol"])
	if h == "" {
		return nil
	}
	protocols := strings.Split(h, ",")
	for i := range protocols {
		protocols[i] = strings.TrimSpace(protocols[i])
	}
	return protocols
}

// IsWebSocketUpgrade returns true if the client requested upgrade to the
// WebSocket protocol.
func IsWebSocketUpgrade(header *SyncMap) bool {
	return tokenListContainsValue(header, "connection", "upgrade") &&
		tokenListContainsValue(header, "upgrade", "websocket")
}

// CheckSameOrigin returns true if the origin is not set or is equal to the request host.
func CheckSameOrigin(header map[string]string) bool {
	origin := header["origin"]
	if origin == "" {
		return true
	}
	u, err := url.Parse(origin)
	if err != nil {
		return false
	}
	return u.Host == header["host"]
}

func (u *Upgrader) selectSubprotocol(header map[string]string) string {
	if u.Subprotocols == nil {
		return ""
	}
	clientProtocols := Subprotocols(header)
	for _, serverProtocol := range u.Subprotocols {
		for _, clientProtocol := range clientProtocols {
			if clientProtocol == serverProtocol {
				return clientProtocol
			}
		}
	}
	return ""
}

func (u *Upgrader) returnError(netConn net.Conn, status int, reason string) (*websocket.Conn, error) {
	err := HandshakeError{reason}

	resp := NewMessage()
	resp.Status = status
	resp.SetHeader("sec-websocket-version", "13")

	bufWrite := new(bytes.Buffer)
	resp.EncodeMessage(bufWrite)
	netConn.Write(bufWrite.Bytes())

	return nil, err
}

// Upgrade upgrades TCP connection to the WebSocket protocol.
//
// The responseHeader is included in the response to the client's upgrade
// request. Use the responseHeader to specify cookies (Set-Cookie) and the
// application negotiated subprotocol (Sec-Websocket-Protocol).
//
// If the upgrade fails, then Upgrade replies to the client with an HTTP error response.
func (u *Upgrader) Upgrade(netConn net.Conn, req *Message) (*websocket.Conn, error) {
	if req.Method != "GET" {
		return u.returnError(netConn, 405, "websocket: not a websocket handshake: request method is not GET")
	}

	if !tokenListContainsValue(&req.Header, "connection", "upgrade") {
		return u.returnError(netConn, 400, "websocket: not a websocket handshake: 'upgrade' token not found in 'Connection' header")
	}

	if !tokenListContainsValue(&req.Header, "upgrade", "websocket") {
		return u.returnError(netConn, 400, "websocket: not a websocket handshake: 'websocket' token not found in 'Upgrade' header")
	}

	if !tokenListContainsValue(&req.Header, "sec-websocket-version", "13") {
		return u.returnError(netConn, 400, "websocket: unsupported version: 13 not found in 'Sec-Websocket-Version' header")
	}

	header := req.Header.Copy()
	checkOrigin := u.CheckOrigin
	if checkOrigin != nil && !checkOrigin(header) {
		return u.returnError(netConn, 403, "websocket: 'Origin' header value not allowed")
	}

	challengeKey := req.GetHeader("sec-websocket-key")
	if challengeKey == "" {
		return u.returnError(netConn, 400, "websocket: not a websocket handshake: `Sec-Websocket-Key' header is missing or blank")
	}

	subprotocol := u.selectSubprotocol(header)
	var (
		err error
	)
	wsConn := websocket.NewConn(netConn, true, u.ReadBufferSize, u.WriteBufferSize)
	wsConn.SetSubprotocol(subprotocol)

	resp := NewMessage()
	resp.Status = 101
	resp.SetHeader("Upgrade", "websocket")
	resp.SetHeader("Connection", "Upgrade")
	resp.SetHeader("Sec-WebSocket-Accept", computeAcceptKey(challengeKey))
	if subprotocol != "" {
		resp.SetHeader("Sec-Websocket-Protocol", wsConn.Subprotocol())
	}
	netConn.SetDeadline(time.Time{})
	if u.HandshakeTimeout > 0 {
		netConn.SetWriteDeadline(time.Now().Add(u.HandshakeTimeout))
	}

	bufWrite := new(bytes.Buffer)
	resp.EncodeMessage(bufWrite)
	netConn.Write(bufWrite.Bytes())

	return wsConn, err
}

//////////////////////////////////////////////////////////////////////////////////////////////////////
var keyGUID = []byte("258EAFA5-E914-47DA-95CA-C5AB0DC85B11")

func computeAcceptKey(challengeKey string) string {
	h := sha1.New()
	h.Write([]byte(challengeKey))
	h.Write(keyGUID)
	return base64.StdEncoding.EncodeToString(h.Sum(nil))
}

func generateChallengeKey() (string, error) {
	p := make([]byte, 16)
	if _, err := io.ReadFull(rand.Reader, p); err != nil {
		return "", err
	}
	return base64.StdEncoding.EncodeToString(p), nil
}

// Octet types from RFC 2616.
var octetTypes [256]byte

const (
	isTokenOctet = 1 << iota
	isSpaceOctet
)

func init() {
	// From RFC 2616
	//
	// OCTET      = <any 8-bit sequence of data>
	// CHAR       = <any US-ASCII character (octets 0 - 127)>
	// CTL        = <any US-ASCII control character (octets 0 - 31) and DEL (127)>
	// CR         = <US-ASCII CR, carriage return (13)>
	// LF         = <US-ASCII LF, linefeed (10)>
	// SP         = <US-ASCII SP, space (32)>
	// HT         = <US-ASCII HT, horizontal-tab (9)>
	// <">        = <US-ASCII double-quote mark (34)>
	// CRLF       = CR LF
	// LWS        = [CRLF] 1*( SP | HT )
	// TEXT       = <any OCTET except CTLs, but including LWS>
	// separators = "(" | ")" | "<" | ">" | "@" | "," | ";" | ":" | "\" | <">
	//              | "/" | "[" | "]" | "?" | "=" | "{" | "}" | SP | HT
	// token      = 1*<any CHAR except CTLs or separators>
	// qdtext     = <any TEXT except <">>

	for c := 0; c < 256; c++ {
		var t byte
		isCtl := c <= 31 || c == 127
		isChar := 0 <= c && c <= 127
		isSeparator := strings.IndexRune(" \t\"(),/:;<=>?@[]\\{}", rune(c)) >= 0
		if strings.IndexRune(" \t\r\n", rune(c)) >= 0 {
			t |= isSpaceOctet
		}
		if isChar && !isCtl && !isSeparator {
			t |= isTokenOctet
		}
		octetTypes[c] = t
	}
}

func skipSpace(s string) (rest string) {
	i := 0
	for ; i < len(s); i++ {
		if octetTypes[s[i]]&isSpaceOctet == 0 {
			break
		}
	}
	return s[i:]
}

func nextToken(s string) (token, rest string) {
	i := 0
	for ; i < len(s); i++ {
		if octetTypes[s[i]]&isTokenOctet == 0 {
			break
		}
	}
	return s[:i], s[i:]
}

// tokenListContainsValue returns true if the 1#token header with the given
// name contains token.
func tokenListContainsValue(header *SyncMap, name string, value string) bool {
	header.RLock()
	defer header.RUnlock()
	val, ok := header.Map[name]
	if !ok {
		return false
	}
	s, ok := val.(string)
	if !ok {
		return false
	}

	for {
		var t string
		t, s = nextToken(skipSpace(s))
		if t == "" {
			break
		}
		s = skipSpace(s)
		if s != "" && s[0] != ',' {
			break
		}
		if strings.EqualFold(t, value) {
			return true
		}
		if s == "" {
			break
		}
		s = s[1:]
	}
	return false
}
