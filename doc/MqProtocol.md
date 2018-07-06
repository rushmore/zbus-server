

# MQ Protocol

zbus MQ protocol is pretty simple, by following HTTP format, with zbus control syntax inside of the HTTP headers.

Websocket by default embbeded in the transport stack sharing same TCP port, zbus automatically detect the transport protocol.
The rule is straightforward, when websocket step in, HTTP message is translated to JSON format, which is handy to stringify back to
standard HTTP message.


## Common format [HTTP JSON Format]

	{ 
		headers: {
			cmd:       pub|sub|create|remove|query|ping    
			id:        <message_id>,
			apiKey:    <apid_key>,
			signature: <signature>
		},

		status:    200|400|404|403|500 ...     //[required], standsfor response

		url:       <url_string>,               //[optional]
		method:    <http_method>,              //[optional] 
		
		body:   <body>                         //[optional]
	} 

All requests to zbus should have id field in headers (optional), when auth required, both apiKey and signature are required.

Signature generation algorithm

	1) sort key ascending in request (recursively on both key and value), and generate json string
	2) Init HmacSHA256 with secretKey, do encrypt on 1)'s json string to generate bytes
	3) signature = Hex format in upper case on the 2)'s bytes

## Publish Message Headers

Request

	{
		headers: {
			cmd:         pub,         //required
			mq:          <mq_name>,   //required  
		},
		body: <body>
	}

Response

	{
		status:      200|400|403|500....,    
		body:        <body> 
	}

## Subscribe Message 

Request

	{
		headers: {
			cmd:         sub,           //required
			mq:          <mq_name>,     //required  
			channel:     <channel_name> //required
			window:      <window_size>
		}
	}

Response

	First message: indicates subscribe success or failure
	{
		status:      200|400|403|500,    
		body:        <string_response> 
	}

	Following messages:
	{
		headers: {
			mq:          <mq_name>,     //required  
			channel:     <channel_name> //required
			sender:      <message from>
			id:          <message id>
		}, 
		body:        <business_data>
	}

## Take Message 

Request

	{
		headers: {
			cmd:         take,          //required
			mq:          <mq_name>,     //required 
			channel:     <channel_name> //required
			window:      <batch_size>
		} 
	}

Response

	{
		status:      200|400|403|500|604, //604 stands for NO data   
		body:        <data> 
	}


## Create MQ/Channel 

Request

	{
		headers: {
			cmd:         create,         //required
			mq:          <mq_name>,      //required

			mqType:      memory|disk|db, //default to memory
			mqMask:      <mask_integer>,
			channel:     <channel_name>,
			channelMask: <mask_integer>,
			offset:      <channel_offset>,
			checksum:    <offset_checksum>
			topic:       <channel_topic>, 
		} 
	}

Response

	{
		status:      200|400|403|500,    
		body:        <message_response> 
	}

## Remove MQ/Channel 

Request

	{
		headers: {
			cmd:         remove,         //required
			mq:          <mq_name>,      //required 
			channel:     <channel_name> 
		} 
	}

Response

	{
		status:      200|400|403|500,    
		body:        <message_response> 
	}


## Query MQ/Channel 

Request

	{
		headers: {
			cmd:         query,          //required
			mq:          <mq_name>,      //required 
			channel:     <channel_name> 
		} 
	}

Response

	{
		status:      200|400|403|500,    
		body:        <mq_channel_info> 
	}
    
	Example
	{
		body: {
			channels: [ ],
			mask: 0,
			name: "DiskQ",
			size: 200000,
			type: "disk"
		},
		status: 200
	}
