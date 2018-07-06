package io.zbus.auth;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import io.zbus.kit.CryptoKit;
import io.zbus.transport.Message;

public class DefaultSign implements RequestSign {  
	
	public String calcSignature(Message request, String apiKey, String secret) {  
		Message copy = new Message(request);
    	copy.setHeader(APIKEY, apiKey);
    	copy.removeHeader(SIGNATURE);
    	String message = JSON.toJSONString(copy, SerializerFeature.MapSortField); //Sort map by key 
		String sign = CryptoKit.hmacSign(message, secret); 
		return sign;
    }
	
	public void sign(Message request, String apiKey, String secret) { 
		String sign = calcSignature(request, apiKey, secret);
		request.setHeader(APIKEY, apiKey); 
		request.setHeader(SIGNATURE, sign);
    }   
}
