function Protocol() { }

Protocol.VERSION_VALUE = "0.8.0";       //start from 0.8.0 

/////////////////////////Command Values/////////////////////////
//!!!Javascript message control: xxx_yyy = xxx-yyy (underscore equals dash), both support
//MQ Produce/Consume
Protocol.PRODUCE = "produce";
Protocol.CONSUME = "consume";
Protocol.RPC = "rpc";
Protocol.ROUTE = "route";     //route back message to sender, designed for RPC

//Topic/ConsumeGroup control
Protocol.DECLARE = "declare";
Protocol.QUERY = "query";
Protocol.REMOVE = "remove";
Protocol.EMPTY = "empty";

//Tracker
Protocol.TRACK_PUB = "track_pub";
Protocol.TRACK_SUB = "track_sub";

Protocol.COMMAND = "cmd";
Protocol.TOPIC = "topic";
Protocol.TOPIC_MASK = "topic_mask";
Protocol.TAG = "tag";
Protocol.OFFSET = "offset";

Protocol.CONSUME_GROUP = "consume_group";
Protocol.GROUP_START_COPY = "group_start_copy";
Protocol.GROUP_START_OFFSET = "group_start_offset";
Protocol.GROUP_START_MSGID = "group_start_msgid";
Protocol.GROUP_START_TIME = "group_start_time";
Protocol.GROUP_WINDOW = "consume_window";
Protocol.GROUP_FILTER_TAG = "group_filter";
Protocol.GROUP_MASK = "group_mask";

Protocol.SENDER = "sender";
Protocol.RECVER = "recver";
Protocol.ID = "id";

Protocol.SERVER = "server";
Protocol.ACK = "ack";
Protocol.ENCODING = "encoding";

Protocol.ORIGIN_ID = "origin_id";     //original id, TODO compatible issue: rawid
Protocol.ORIGIN_URL = "origin_url";    //original URL  
Protocol.ORIGIN_STATUS = "origin_status"; //original Status  TODO compatible issue: reply_code

//Security 
Protocol.TOKEN = "token";


/////////////////////////Flag values/////////////////////////	
Protocol.MASK_PAUSE = 1 << 0;
Protocol.MASK_RPC = 1 << 1;
Protocol.MASK_EXCLUSIVE = 1 << 2;
Protocol.MASK_DELETE_ON_EXIT = 1 << 3;


////////////////////////////////////////////////////////////

function hashSize(obj) {
    var size = 0, key;
    for (key in obj) {
        if (obj.hasOwnProperty(key)) size++;
    }
    return size;
}
function randomKey (obj) {
    var keys = Object.keys(obj)
    return keys[keys.length * Math.random() << 0];
};
function clone(obj) {
    if (null == obj || "object" != typeof obj) return obj;
    var copy = obj.constructor();
    for (var attr in obj) {
        if (obj.hasOwnProperty(attr)) copy[attr] = obj[attr];
    }
    return copy;
}

function uuid() {
    //http://stackoverflow.com/questions/105034/how-to-create-a-guid-uuid-in-javascript
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

String.prototype.format = function () {
    var args = arguments;
    return this.replace(/{(\d+)}/g, function (match, number) {
        return typeof args[number] != 'undefined' ? args[number] : match;
    });
}

Date.prototype.format = function (fmt) { //author: meizz 
    var o = {
        "M+": this.getMonth() + 1,
        "d+": this.getDate(),
        "h+": this.getHours(),
        "m+": this.getMinutes(),
        "s+": this.getSeconds(),
        "q+": Math.floor((this.getMonth() + 3) / 3),
        "S": this.getMilliseconds()
    };
    if (/(y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
    for (var k in o)
        if (new RegExp("(" + k + ")").test(fmt)) fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
    return fmt;
}

function camel2underscore(key) {
    return key.replace(/\.?([A-Z])/g, function (x, y) { return "_" + y.toLowerCase() }).replace(/^_/, "");
}

function underscore2camel(key) {
    return key.replace(/_([a-z])/g, function (g) { return g[1].toUpperCase(); });
} 

var HttpStatus = {
    200: "OK",
    201: "Created",
    202: "Accepted",
    204: "No Content",
    206: "Partial Content",
    301: "Moved Permanently",
    304: "Not Modified",
    400: "Bad Request",
    401: "Unauthorized",
    403: "Forbidden",
    404: "Not Found",
    405: "Method Not Allowed",
    416: "Requested Range Not Satisfiable",
    500: "Internal Server Error"
};


var __NODEJS__ = typeof module !== 'undefined' && module.exports;

if (__NODEJS__) {

var Events = require('events');
var Buffer = require("buffer").Buffer;
var Socket = require("net");
var inherits = require("util").inherits;
var tls = require("tls");
var fs = require("fs");
    
function string2Uint8Array(str, encoding) {
    var buf = Buffer.from(str, encoding); 
    var data = new Uint8Array(buf.length);
    for (var i = 0; i < buf.length; ++i) {
        data[i] = buf[i];
    }
    return data;
}

function uint8Array2String(data, encoding) {
    var buf = Buffer.from(data);
	return buf.toString(encoding);
} 
    
} else { //WebSocket
	
function inherits(ctor, superCtor) {
	ctor.super_ = superCtor;
	ctor.prototype = Object.create(superCtor.prototype, {
	    constructor: {
	        value: ctor,
	        enumerable: false,
	        writable: true,
	        configurable: true
	    }
	});
};
	
	
function string2Uint8Array(str, encoding) {
	var encoder = new TextEncoder(encoding);
	return encoder.encode(str);
}

function uint8Array2String(buf, encoding) {
	var decoder = new TextDecoder(encoding);
	return decoder.decode(buf);
}

} 

/** 
    * msg.encoding = "utf8"; //encoding on message body
    * msg.body can be 
    * 1) ArrayBuffer, Uint8Array, Init8Array to binary data
    * 2) string value
    * 3) otherwise, JSON converted inside
    * 
    * @param msg
    * @returns ArrayBuffer
    */
function httpEncode(msg) {
    var headers = "";
    var encoding = msg.encoding;
    if (!encoding) encoding = "utf8"; 

    var body = msg.body;
    var contentType = msg["content-type"];
    if (body) {
        if (body instanceof ArrayBuffer) {
            body = new Uint8Array(body);
        } else if (body instanceof Uint8Array || body instanceof Int8Array) {
            //no need to handle
        } else {
            if (typeof body != 'string') {
                body = JSON.stringify(body);
                contentType = "application/json";
            } 
            body = string2Uint8Array(body, encoding);
        }
    } else {
        body = new Uint8Array(0);
    }
    msg["content-length"] = body.byteLength;
    msg["content-type"] = contentType;

    var nonHeaders = { 'status': true, 'method': true, 'url': true, 'body': true }
    var line = ""
    if (msg.status) {
        var desc = HttpStatus[msg.status];
        if (!desc) desc = "Unknown Status";
        line = "HTTP/1.1 {0} {1}\r\n".format(msg.status, desc);
    } else {
        var method = msg.method;
        if (!method) method = "GET";
        var url = msg.url;
        if (!url) url = "/";
        line = "{0} {1} HTTP/1.1\r\n".format(method, url);
    }

    headers += line;
    for (var key in msg) {
        if (key in nonHeaders) continue;
        var val = msg[key];
        if (typeof val == 'undefined') continue;
        line = "{0}: {1}\r\n".format(camel2underscore(key), msg[key]);
        headers += line;
    }
    headers += "\r\n";

    delete msg["content-length"]; //clear
    delete msg["content-type"]

    var headerBuffer = string2Uint8Array(headers, encoding);
    var headerLen = headerBuffer.byteLength;
    //merge header and body
    var buffer = new Uint8Array(headerBuffer.byteLength + body.byteLength);  
    for (var i = 0; i < headerBuffer.byteLength; i++) {
        buffer[i] = headerBuffer[i];
    }

    for (var i = 0; i < body.byteLength; i++) {
        buffer[headerLen + i] = body[i];
    }
    return buffer;
};
 
function httpDecode(data) {
    var encoding = "utf8";
    if (typeof data == "string") {
        data = string2Uint8Array(data, encoding);
    } else if (data instanceof Uint8Array || data instanceof Int8Array) {
        //ignore
    } else if (data instanceof ArrayBuffer) {
        data = new Uint8Array(data);
    } else {
        //not support type
        return null;
    }
    var i = 0, pos = -1;
    var CR = 13, NL = 10;
    while (i + 3 < data.byteLength) {
        if (data[i] == CR && data[i + 1] == NL && data[i + 2] == CR && data[i + 3] == NL) {
            pos = i;
            break;
        }
        i++;
    }
    if (pos == -1) return null;

    var str = uint8Array2String(data.slice(0, pos), encoding);

    var blocks = str.split("\r\n");
    var lines = [];
    for (var i in blocks) {
        var line = blocks[i];
        if (line == '') continue;
        lines.push(line);
    }

    var msg = {};
    //parse first line 
    var bb = lines[0].split(" ");
    if (bb[0].toUpperCase().startsWith("HTTP")) {
        msg.status = parseInt(bb[1]);
    } else {
        msg.method = bb[0];
        msg.url = bb[1];
    }
    var typeKey = "content-type";
    var typeVal = "text/plain";
    var lenKey = "content-length";
    var lenVal = 0;

    for (var i = 1; i < lines.length; i++) {
        var line = lines[i];
        var p = line.indexOf(":");
        if (p == -1) continue;
        var key = line.substring(0, p).trim().toLowerCase();
        var val = line.substring(p + 1).trim();
        if (key == lenKey) {
            lenVal = parseInt(val);
            continue;
        }
        if (key == typeKey) {
            typeVal = val;
            continue;
        }

        key = underscore2camel(key);
        msg[key] = val;
    }
    if (pos + 4 + lenVal > data.byteLength) {
        return null;
    }
    //mark consumed Length
    data.consumedLength = pos + 4 + lenVal;

    var encoding = msg.encoding;
    if (!encoding) encoding = "utf8"; 
    var bodyData = data.slice(pos + 4, pos + 4 + lenVal);
    if (typeVal == "text/html" || typeVal == "text/plain") {
        msg.body = uint8Array2String(bodyData, encoding);
    } else if (typeVal == "application/json") {
        var bodyString = uint8Array2String(bodyData, encoding);
        msg.body = JSON.parse(bodyString);
    } else {
        msg.body = bodyData;
    }

    return msg;
}


function Ticket(reqMsg, callback) {
    this.id = uuid();
    this.request = reqMsg;
    this.response = null;
    this.callback = callback;
    reqMsg.id = this.id;
} 

function addressKey(serverAddress) {
    var scheme = ""
    if (serverAddress.sslEnabled) {
        scheme = "[SSL]"
    }
    return scheme + serverAddress.address;
}

function normalizeAddress(serverAddress, certFile) {
    if (typeof (serverAddress) == 'string') {
        var sslEnabled = false;
        if (certFile) sslEnabled = true;

        serverAddress = serverAddress.trim();
        if (serverAddress.startsWith("ws://")) {
            serverAddress = serverAddress.substring(5);
        }
        if (serverAddress.startsWith("wss://")) {
            serverAddress = serverAddress.substring(6);
            sslEnabled = true;
        }

        serverAddress = {
            address: serverAddress,
            sslEnabled: sslEnabled,
        }; 
    }
    return serverAddress; 
}

function containsAddress(serverAddressArray, serverAddress) {
    for (var i in serverAddressArray) {
        var addr = serverAddressArray[i];
        if (addr.address == serverAddress.address && addr.sslEnabled == serverAddress.sslEnabled) {
            return true;
        }
    }
    return false;
}



 
function MessageClient(serverAddress, certFile) { 
    if (__NODEJS__) {
        Events.EventEmitter.call(this);
    }
    serverAddress = normalizeAddress(serverAddress, certFile);
    this.serverAddress = serverAddress;
    this.certFile = certFile;

    var address = this.serverAddress.address;
    var p = address.indexOf(':');
    if (p == -1) {
        this.serverHost = address.trim();
        this.serverPort = 80;
    } else {
        this.serverHost = address.substring(0, p).trim();
        this.serverPort = parseInt(address.substring(p + 1).trim());
    }

    this.autoReconnect = true;
    this.reconnectInterval = 3000;
    this.ticketTable = {};
    this.onMessage = null;
    this.onConnected = null;
    this.onDisconnected = null; 

    this.readBuf = new Uint8Array(0); //only used when NODEJS 
} 

if (__NODEJS__) {
	
inherits(MessageClient, Events.EventEmitter);

MessageClient.prototype.connect = function (connectedHandler) { 
    console.log("Trying to connect: " + this.serverHost + ":" + this.serverPort);
    var certFile = this.certFile;
    if(this.serverAddress.sslEnabled) { 
        console.log("cert:" + this.certFile);
        this.socket = tls.connect(this.serverPort, {  
            cert: fs.readFileSync(certFile),  
            ca: fs.readFileSync(certFile),
        });
    } else {
        this.socket = Socket.connect({ host: this.serverHost, port: this.serverPort }); 
    } 

    var client = this;
    this.socket.on("connect", function () {
        console.log("MessageClient connected: " + client.serverHost + ":" + client.serverPort);
        if (connectedHandler) {
            connectedHandler();
        }
       
        client.heartbeatInterval = setInterval(function () {
            var msg = {};
            msg.cmd = 'heartbeat';
            client.invoke(msg);
        }, 300 * 1000);
    }); 

    this.socket.on("error", function (error) {
        client.emit("error", error);
    });

    this.socket.on("close", function () {
        client.socket.destroy();
        clearInterval(client.heartbeatInterval);

        if (client.onDisconnected) {
            client.onDisconnected();
            return;
        }  

        if (client.autoReconnect) {
            console.log("Trying to recconnect: " + client.serverHost + ":" + client.serverPort);
            setTimeout(function () {
                client.connect(connectedHandler);
            }, client.reconnectInterval);
        }
    });

    client.on("error", function (error) {
        console.log(error);
    });

    this.socket.on("data", function (data) { 
        var encoding = "utf8";
        if (typeof data == "string") {
            data = string2Uint8Array(data, encoding);
        } else if (data instanceof Uint8Array || data instanceof Int8Array) {
            //ignore
        } else if (data instanceof ArrayBuffer) {
            data = new Uint8Array(data);
        } else {
            throw "Invalid data type: " + data;
        }
        //concate data
        var bufLen = client.readBuf.byteLength;
        var readBuf = new Uint8Array(bufLen + data.byteLength);
        for (var i = 0; i < bufLen; i++) {
            readBuf[i] = client.readBuf[i];
        }
        for (var i = 0; i < data.byteLength; i++) {
            readBuf[i + bufLen] = data[i];
        }
        client.readBuf = readBuf;

        while (true) {
            if (client.readBuf.consumedLength) {
                client.readBuf = client.readBuf.slice(client.readBuf.consumedLength);
            }
            var msg = httpDecode(client.readBuf);
            if (msg == null) break;
            var msgid = msg.id;
            var ticket = client.ticketTable[msgid];
            if (ticket) {
                ticket.response = msg;
                if (ticket.callback) {
                    ticket.callback(msg);
                }
                delete client.ticketTable[msgid];
            } else if (client.onMessage) {
                client.onMessage(msg);
            }
        } 
    });
};

MessageClient.prototype.invoke = function (msg, callback) {
    if (callback) {
        var ticket = new Ticket(msg, callback);
        this.ticketTable[ticket.id] = ticket;
    }
    var buf = httpEncode(msg); 
    this.socket.write(Buffer.from(buf));
};

MessageClient.prototype.close = function () {
    if (this.socket.destroyed) return;
    clearInterval(this.heartbeatInterval);
    this.socket.removeAllListeners("close");
    this.autoReconnect = false;
    this.socket.destroy();
} 

} else { 
//WebSocket
	
MessageClient.prototype.connect = function (connectedHandler) {
    console.log("Trying to connect to " + this.address());
    if (!connectedHandler) {
        connectedHandler = this.onConnected;
    }

    var WebSocket = window.WebSocket;
    if (!WebSocket) {
        WebSocket = window.MozWebSocket;
    }

    this.socket = new WebSocket(this.address());
    this.socket.binaryType = 'arraybuffer';
    var client = this;
    this.socket.onopen = function (event) {
        console.log("Connected to " + client.address());
        if (connectedHandler) {
            connectedHandler(event);
        }
        client.heartbeatInterval = setInterval(function () {
            var msg = {};
            msg.cmd = "heartbeat";
            client.invoke(msg);
        }, 30 * 1000);
    };

    this.socket.onclose = function (event) {
        clearInterval(client.heartbeatInterval);
        if (client.onDisconnected) {
            client.onDisconnected(); 
        }
        if (client.autoReconnect) {
            setTimeout(function () {
                try { client.connect(connectedHandler); } catch (e) { }//ignore
            }, client.reconnectInterval);
        }
    };

    this.socket.onmessage = function (event) {
        var msg = httpDecode(event.data);
        var msgid = msg.id;
        var ticket = client.ticketTable[msgid];
        if (ticket) {
            ticket.response = msg;
            if (ticket.callback) {
                ticket.callback(msg);
            }
            delete client.ticketTable[msgid];
        } else if (client.onMessage) {
            client.onMessage(msg);
        }  
    }

    this.socket.onerror = function (data) {
        console.log("Error: " + data);
    }
}

MessageClient.prototype.invoke = function (msg, callback) {
    if (!this.socket || this.socket.readyState != WebSocket.OPEN) {
        throw "socket is not open, invalid"; 
    }
    if (callback) {
        var ticket = new Ticket(msg, callback);
        this.ticketTable[ticket.id] = ticket;
    }

    var buf = httpEncode(msg);
    this.socket.send(buf);
};

MessageClient.prototype.close = function () {
    clearInterval(this.heartbeatInterval);
    this.socket.onclose = function () { }
    this.autoReconnect = false;
    this.socket.close();
}

MessageClient.prototype.address = function () {
    if (this.serverAddress.sslEnabled) {
        return "wss://" + this.serverAddress.address;
    }
    return "ws://" + this.serverAddress.address;
}
 
}//End of __NODEJS___



MessageClient.prototype.sendMessage = function (msg) {
    this.invoke(msg);
} 

function MqClient(serverAddress, certFile) {
    MessageClient.call(this, serverAddress, certFile);
    this.token = null;
}
inherits(MqClient, MessageClient) 

MqClient.prototype.fork = function () {
    return new MqClient(this.serverAddress, this.certFile);
}

MqClient.prototype._invoke = function (cmd, msg, callback) {
    if (typeof (msg) == 'string') msg = { topic: msg };  //assume to be topic
    if (!msg) msg = {};

    if (this.token) msg.token = this.token;
    msg.cmd = cmd; 
    this.invoke(msg, callback);
}
 
MqClient.prototype.query = function (msg, callback) { 
    this._invoke(Protocol.QUERY, msg, callback);
}
MqClient.prototype.declare = function (msg, callback) {
    this._invoke(Protocol.DECLARE, msg, callback);
} 
MqClient.prototype.remove = function (msg, callback) {
    this._invoke(Protocol.REMOVE, msg, callback);
}
MqClient.prototype.empty = function (msg, callback) {
    this._invoke(Protocol.EMPTY, msg, callback);
}

MqClient.prototype.produce = function (msg, callback) {
    this._invoke(Protocol.PRODUCE, msg, callback); 
}

MqClient.prototype.consume = function (msg, callback) { 
    this._invoke(Protocol.CONSUME, msg, callback);
}
MqClient.prototype.route = function (msg, callback) {
    msg.ack = false;
    if (msg.status) {
        msg.originStatus = msg.status;
        delete msg.status;
    }
    this._invoke(Protocol.ROUTE, msg, callback);
}



function BrokerRouteTable() {
    this.serverTable = {};  //{ ServerAddress => ServerInfo }
    this.topicTable = {};   //{ TopicName=>{ ServerAddres=>TopicInfo } }
    this.votesTable = {};   //{ TrackerAddress => { servers: Set[ServerAddress], version: xxx, tracker: ServerAddress } }
    this.voteFactor = 0.5;  //for a serverInfo, how much percent of trackers should have voted
}


BrokerRouteTable.prototype.updateTracker = function (trackerInfo) {
    var toRemove = [];
    //1) Update VotesTable
    var trackedServerList = [];
    for(var serverAddr in trackerInfo.serverTable){
    	var serverInfo = trackerInfo.serverTable[serverAddr];
    	trackedServerList.push(serverInfo.serverAddress);
    }
    var trackerFullAddr = addressKey(trackerInfo.serverAddress); 
    if (trackerFullAddr in this.votesTable) {
        var vote = this.votesTable[trackerFullAddr];
        if (vote.infoVersion >= trackerInfo.infoVersion) { 
            return toRemove; //No need to update for older version
        }  
        //update votesTable if trackerInfo is newer updates
        vote.infoVersion = trackerInfo.version,
        vote.servers = trackedServerList;
    } else {
        this.votesTable[trackerFullAddr] = {
            version: trackerInfo.infoVersion,
            servers: trackedServerList,
            tracker: trackerInfo.serverAddress,
        };
    }
     
    //2) Merge ServerTable
    var newServerTable = trackerInfo.serverTable;
    for (var serverAddr in newServerTable) {
        var newServerInfo = newServerTable[serverAddr];
        var serverInfo = this.serverTable[serverAddr];
        if (serverInfo && serverInfo.infoVersion >= newServerInfo.infoVersion) {
            continue;
        }
        this.serverTable[serverAddr] = newServerInfo;
    } 
    
    //3) Purge ServerTable
    toRemove = this._purge();
    return toRemove;
}


BrokerRouteTable.prototype.removeTracker = function (trackerAddress) {
    var trackerFullAddr = addressKey(trackerAddress);
    if (!(trackerFullAddr in this.votesTable)) {
        return [];
    }
    delete this.votesTable[trackerFullAddr];
    return this._purge();
} 

BrokerRouteTable.prototype._purge = function () {
    var toRemove = [];
    //Find servers to remove
    for (var serverAddr in this.serverTable) {
        var serverInfo = this.serverTable[serverAddr];
        var serverAddress = serverInfo.serverAddress;
        
        var count = 0;
        for (var trackerAddr in this.votesTable) {//count on votesTable
            var vote = this.votesTable[trackerAddr];
            if (containsAddress(vote.servers, serverAddress)) count++; 
        }
        var voterCount = hashSize(this.votesTable);
        if (count < voterCount * this.voteFactor) {
            toRemove.push(serverAddress);
        }
    }
    //Remove servers
    for (var i in toRemove) {
        var serverAddress = toRemove[i];
        var addrKey = addressKey(serverAddress);
        delete this.serverTable[addrKey];
    }

    this._rebuildTopicTable();
    return toRemove;
}


BrokerRouteTable.prototype._rebuildTopicTable = function () { 
    this.topicTable = {};
    for (var fullAddr in this.serverTable) {
        var serverInfo = this.serverTable[fullAddr]; 
        var serverTopicTable = serverInfo.topicTable;
        for (var topic in serverTopicTable) {
            var serverTopicInfo = serverTopicTable[topic];
            if (!(topic in this.topicTable)) {
                this.topicTable[topic] = [serverTopicInfo];
            } else {
                this.topicTable[topic].push(serverTopicInfo);
            }
        }
    }
}


function Broker(trackerAddressList) {
    this.trackerSubscribers = {};
    this.clientTable = {};
    this.routeTable = new BrokerRouteTable();  

    this.sslCertFileTable = {};
    this.defaultSslCertFile = null;

    this.onServerJoin = null;
    this.onServerLeave = null;
    this.onServerUpdated = null;

    this.readyServerRequired = 0;
    this.readyTriggered = false;
    this.onReady = null;
    this.readyTimeout = 3000;//3 seconds

    if (trackerAddressList) {
    	var bb = trackerAddressList.split(";");
    	for(var i in bb){
    		var trackerAddress = bb[i];
    		if(trackerAddress.trim() == "") continue;;
    		this.addTracker(trackerAddress);
    	} 
    } 
}

Broker.prototype.ready = function (readyHandler) {
    this.onReady = readyHandler;
    if (this.readyTriggered) {
        if(this.onReady) this.onReady();
    }
}

Broker.prototype.select = function (serverSelector, topic) {
    keys = serverSelector(this.routeTable, topic);
    var clients = [];
    for (var i in keys) {
        var key = keys[i];
        if (key in this.clientTable) {
            clients.push(this.clientTable[key]);
        }
    }
    return clients;
}

Broker.prototype.addTracker = function (trackerAddress, certFile) {
    trackerAddress = normalizeAddress(trackerAddress, certFile);
    var fullAddr = addressKey(trackerAddress);
    if (certFile) {
        this.sslCertFileTable[trackerAddress.address] = certFile;
    }

    if (fullAddr in this.trackerSubscribers) return;

    var client = this._createClient(trackerAddress, certFile);
    this.trackerSubscribers[fullAddr] = client;

    var broker = this; 
    var remoteTrackerAddress = trackerAddress;
    client.connect(function (event) {
        var msg = {};
        msg.cmd = Protocol.TRACK_SUB;
        client.sendMessage(msg);
    });

    client.onDisconnected = function(){
        var toRemove = broker.routeTable.removeTracker(remoteTrackerAddress);
        for (var i in toRemove) {
            var serverAddress = toRemove[i];
            broker._removeServer(serverAddress); 
        }
    }

    client.onMessage = function (msg) {
        if (msg.status != "200") {
            console.log("Warn: " + msg);
            return;
        }
        var trackerInfo = msg.body; 
        //update tracker address
        remoteTrackerAddress = trackerInfo.serverAddress;
        broker.sslCertFileTable[trackerInfo.serverAddress.address] = certFile

        var toRemove = broker.routeTable.updateTracker(trackerInfo);
        if (!broker.readyTriggered) {
            broker.readyServerRequired = hashSize(trackerInfo.serverTable);
        }
        var serverTable = broker.routeTable.serverTable;
        for (var serverAddr in serverTable) {
            var serverInfo = serverTable[serverAddr];
            broker._addServer(serverInfo);
        }
        for (var i in toRemove) {
            var serverAddress = toRemove[i];
            broker._removeServer(serverAddress); 
        }
        if (broker.onServerUpdated) {
            broker.onServerUpdated();
        }
    } 

    setTimeout(function () {
        if (!broker.readyTriggered) {
            broker.readyTriggered = true;
            if (broker.onReady) {
                try { broker.onReady(); } catch (e) { console.log(e); }
            }
        }
    }, this.readyTimeout);
}


Broker.prototype._addServer = function (serverInfo) {
    serverAddress = serverInfo.serverAddress; 
    var fullAddr = addressKey(serverAddress);  
    var client = this.clientTable[fullAddr];
    if (client != null) { 
        return;
    } 
    client = this._createClient(serverAddress);
    this.clientTable[fullAddr] = client;
    
    var broker = this;
    client.connect(function () {
        if (broker.onServerJoin) {
            console.log("Server Joined: " + fullAddr);
            broker.onServerJoin(serverInfo);
        }

        broker.readyServerRequired--;
        if (broker.readyServerRequired <= 0) {
            if (!broker.readyTriggered) {
                broker.readyTriggered = true;
                if (broker.onReady) {
                    try { broker.onReady(); } catch (e) { console.log(e); }
                }
            }
        }
    });  
}

Broker.prototype._removeServer = function (serverAddress) { 
    var fullAddr = addressKey(serverAddress);
    var client = this.clientTable[fullAddr];
    if (client == null) {
        return;
    }

    if (this.onServerLeave) {
        console.log("Server Left: " + fullAddr);
        this.onServerLeave(serverAddress);
    }

    client.close();
    delete this.clientTable[fullAddr]; 
}
 

Broker.prototype._createClient = function (serverAddress, certFile) {
    if (serverAddress.sslEnabled) {
        if (!certFile) {
            certFile = this.sslCertFileTable[serverAddress.address];
        }
        if (!certFile) {
            certFile = this.defaultSslCertFile;
        } 
    }
    return new MqClient(serverAddress, certFile);
} 

////////////////////////////////////////////////////////// 
function MqAdmin(broker) {
    this.broker = broker;
    this.adminServerSelector = function (routeTable, topic) {
        return Object.keys(routeTable.serverTable);
    }
}

MqAdmin.prototype._invoke = function (func, msg, serverSelector, callback) {
    if (typeof (msg) == 'string') {
        msg = { topic: msg };
    } 
    var clients = this.broker.select(serverSelector, msg.topic);
    if (clients.length < 1) return;
    if (clients.length == 1) {
        clients[0][func](msg, callback);
        return;
    }
    var promises = [];
    for (var i in clients) {
        var client = clients[i];
        var promise = new Promise(function (resolve, reject) {
            client[func](msg, function (msg) {
                resolve(msg);
            })
        });
        promises.push(promise);
    }
    Promise.all(promises).then(function (values) {
        if(callback) callback(values);
    });
}

MqAdmin.prototype.declare = function (msg, callback) { 
    this._invoke(Protocol.DECLARE, msg, this.adminServerSelector, callback);
}
MqAdmin.prototype.query = function (msg, callback) {
    this._invoke(Protocol.QUERY, msg, this.adminServerSelector, callback);
}
MqAdmin.prototype.remove = function (msg, callback) {
    this._invoke(Protocol.REMOVE, msg, this.adminServerSelector, callback);
}
MqAdmin.prototype.empty = function (msg, callback) {
    this._invoke(Protocol.EMPTY, msg, this.adminServerSelector, callback);
}
 

function Producer(broker) {
    MqAdmin.call(this, broker);
    this.produceServerSelector = function (routeTable, topic) {
        if (hashSize(routeTable.serverTable) == 0) return [];
        var topicList = routeTable.topicTable[topic];
        if (!topicList || topicList.length == 0) { 
            return [randomKey(routeTable.serverTable)]; //random select
        }
        var target = topicList[0];
        for (var i = 1; i < topicList.length; i++) {
            var current = topicList[i];
            if (target.consumerCount < current.consumerCount) {
                target = current;
            }
        }
        return [addressKey(target.serverAddress)];
    }
} 
inherits(Producer, MqAdmin)

Producer.prototype.publish = function (msg, callback) {
    this._invoke(Protocol.PRODUCE, msg, this.produceServerSelector, callback);
} 

 
function Consumer(broker, consumeCtrl) {
    MqAdmin.call(this, broker); 
    this.consumeServerSelector = function (routeTable, topic) {
        return Object.keys(routeTable.serverTable);
    }
    this.connectionCount = 1;
    this.messageHandler = null;
    if (typeof consumeCtrl == 'string') {
        consumeCtrl = {topic: consumeCtrl}
    }
    this.consumeCtrl = consumeCtrl;

    this.consumeClientTable = {}; //addressKey => list of MqClient consuming
} 
inherits(Consumer, MqAdmin);

Consumer.prototype.consumeToServer = function (clientInBrokerTable) {
    var addr = addressKey(clientInBrokerTable.serverAddress);
    if (addr in this.consumeClientTable) return; 

    var consumingClients = [];
    var consumer = this;
    for (var i = 0; i < this.connectionCount; i++) {
        var consumeClient = clientInBrokerTable.fork();
        consumingClients.push(consumeClient); 
        consumer.consume(consumeClient, consumer.consumeCtrl);  
    }
    this.consumeClientTable[addr] = consumingClients; 
}

Consumer.prototype.leaveServer = function (serverAddress) {
    var addr = addressKey(serverAddress);
    var consumingClients = this.consumeClientTable[addr];
    if (!consumingClients) return;
    for (var i in consumingClients) {
        var client = consumingClients[i];
        client.close();
    }
    delete this.consumeClientTable[addr]; 
}

Consumer.prototype.start = function () {
    var clientTable = this.broker.clientTable;
    for (var addr in clientTable) {
        var client = clientTable[addr];
        this.consumeToServer(client)
    }
    var consumer = this;
    this.broker.onServerJoin = function (serverInfo) {
        var addr = addressKey(serverInfo.serverAddress);
        var client = consumer.broker.clientTable[addr];
        if (!client) {
            console.log("WARN: " + addr + " not found in broker clientTable");
            return;
        }
        consumer.consumeToServer(client);
    }
    this.broker.onServerLeave = function (serverAddress) {
        consumer.leaveServer(serverAddress);
    }
}


Consumer.prototype.consume = function(client, consumeCtrl) { 
    var messageHandler = this.messageHandler;
    function consumeCallback(consumeCtrl, res) {
        if (res.status == 404) {
            client.declare(consumeCtrl, function (res) {
                if (res.status != 200) {
                    console.log("declare error: " + res);
                    return;
                }
                client.consume(consumeCtrl, function (res) {
                    consumeCallback(consumeCtrl, res);
                })
            });
            return;
        }
         
        var originUrl = res.originUrl
        var id = res.originId; 
        delete res.originUrl
        delete res.originId
        if (typeof originUrl == "undefined") originUrl = "/";  
        res.id = id;
        res.url = originUrl;

        if (messageHandler) {
            try{
            	messageHandler(res, client);
            } finally {
                client.consume(consumeCtrl, function (res) {
                    consumeCallback(consumeCtrl, res);
                })
            } 
        } 
    }
    client.connect(function(){
        client.declare(consumeCtrl, function(res){
            if (res.status != 200) {
                console.log("declare error: " + res);
                return;
            }
            client.consume(consumeCtrl, function (res) {
                consumeCallback(consumeCtrl,res);
            });
        }); 
    })
}


function RpcInvoker(broker, topic, ctrl) { 
    this.broker = broker;
    this.prodcuer = new Producer(broker); 
    if (ctrl) {
        this.ctrl = clone(ctrl);
        this.ctrl.topic = topic;
    } else {
        this.ctrl = { topic: topic }
    } 
    var invoker = this;

    return new Proxy(this, {
        get: function (target, name) {
            return name in target ? target[name] : invoker._proxyMethod(name);
        }
    });
} 

RpcInvoker.prototype._proxyMethod = function (method) {
    var invoker = this;
    return function () { 
        var callback;
        if(arguments.length > 0){ 
            var callback = arguments[arguments.length - 1];
            if (typeof (callback) != 'function') {
                callback = null;
            }
        }
        var len = arguments.length;
        if (callback) len--;
        var params = [];
        for (var i = 0; i < len; i++) {
            params.push(arguments[i]);
        }
        req = {
            method: method,
            params: params,
        }  
        invoker._invokeMethod(req, callback);
    }
}

RpcInvoker.prototype._invokeMethod = function (req, callback) {
    var msg = clone(this.ctrl);
    msg.body = JSON.stringify(req);
    msg.ack = false;
    this.prodcuer.publish(msg, function (msg) {
        if (callback) {
            if (msg.status != "200") {
                throw msg.body;
            }
            
            if(typeof(msg.body) == 'string'){
            	res = JSON.parse(msg.body);
            } else {
            	res = msg.body;
            }  
            if (res.error) {
                throw res.error;
            }
            callback(res.result);
        }
    });
}

//{ method: xxx, params:[], module: xxx]
RpcInvoker.prototype.invoke = function(){ 
    if (arguments.length < 1) {
        throw "Missing request parameter";
    }
    var req = arguments[0];
    var callback = arguments[arguments.length - 1];
    if (typeof (callback) != 'function') {
        callback = null;
    }

    if (typeof (req) == 'string') { 
        var params = [];
        var len = arguments.length;
        if (callback) len--;
        for (var i = 1; i < len; i++) {
            params.push(arguments[i]);
        }
        req = {
            method: req,
            params: params,
        }
    }

    this._invokeMethod(req, callback);
}


function RpcProcessor() {
    this.methodTable = {}

    var processor = this;
    this.messageHandler = function (msg, client) {
        var res = { error: null };  
        var resMsg = {
            id: msg.id,
            recver: msg.sender,
            topic: msg.topic,
            status: 200,
            body: res,
        }; 
        
        try { 
            var req = msg.body;
            if(typeof(req) == 'string') req = JSON.parse(req);  
            var key = processor._generateKey(req.module, req.method);
            var m = processor.methodTable[key];
            if(m) {
                try {
                    res.result = m.method.apply(m.target, req.params); 
                } catch (e) {
                    res.error = e; 
                }  
            } else {
                res.error = "method(" + key + ") not found";
            } 
        } catch (e) {
            res.error = e; 
        } finally {   
            try { client.route(resMsg); } catch (e) { }
        }
    }
}

RpcProcessor.prototype._generateKey = function(moduleName, methodName){
    if (moduleName) return moduleName + ":" + methodName;
    return ":" + methodName;
}

RpcProcessor.prototype.addModule = function (serviceObj, moduleName) {
    if (typeof (serviceObj) == 'function') {
        var func = serviceObj;
        var key = this._generateKey(moduleName, func.name);
        if (key in this.methodTable) {
            console.log("WARN: " + key + " already exists, will be overriden");
        }
        this.methodTable[key] = { method: func, target: null};
        return;
    }

    for (var name in serviceObj) {
        var func = serviceObj[name];
        if (typeof (func) != 'function') continue;
        var key = this._generateKey(moduleName, name);
        if (key in this.methodTable) {
            console.log("WARN: " + key + " already exists, will be overriden");
        }
        this.methodTable[key] = { method: func, target: serviceObj };
    }
}
  
if (__NODEJS__) {
    module.exports.Protocol = Protocol;
    module.exports.MessageClient = MessageClient;
    module.exports.MqClient = MqClient;
    module.exports.BrokerRouteTable = BrokerRouteTable;
    module.exports.Broker = Broker;
    module.exports.Producer = Producer;
    module.exports.Consumer = Consumer;
    module.exports.RpcInvoker = RpcInvoker;
    module.exports.RpcProcessor = RpcProcessor;
}

 