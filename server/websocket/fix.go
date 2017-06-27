package websocket

import "net"

//NewConn create Websocket connection
func NewConn(conn net.Conn, isServer bool, readBufferSize, writeBufferSize int) *Conn {
	return newConn(conn, isServer, readBufferSize, writeBufferSize)
}

// SetSubprotocol set the subprotocol
func (c *Conn) SetSubprotocol(subprotocol string) {
	c.subprotocol = subprotocol
}
