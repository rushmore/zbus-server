
# RPC Protocol 

## Request

	{ 
		headers: {
			id:        <message_id>,
			apiKey:    <apid_key>,
			signature: <signature>
		},
		url:      <function_url>  //[required]  
		method:   <http_method>,  //[optional]   
		body:     <param_array>   //[optional]
	} 

function url can be any format of HTTP URL, if no configuration, following the simple mapping rules:
	
*/${module}/${language_method_name}[/${param1}/${param2}/.....]*

	If body is populated in request, the params list in URL is ignored
		
If RPC is based on MQ, add MQ prefix in url,

*/${mq}*/${module}/${language_method_name}[/${param1}/${param2}/.....]

or, add 3 key-value pairs in headers

	{
		cmd:      'pub',     
		mq:       <mq_name>,   //which MQ is the RPC based
		ack:      false        //No ACK from zbus for RPC
	}

## Response

	{
		status:      200|400|403|404|500   //required
		body:        <data_or_exception>,
		id:          <message_id>
	}

