package io.zbus.kit;

import java.util.HashMap;
import java.util.Map;

public class StrKit {

	public static boolean isEmpty(String str) {
		return str == null || str.trim().equals(""); 
	}

	public static Map<String, String> kvp(String value){
		return StrKit.kvp(value, "&");
	}

	public static Map<String, String> kvp(String value, String delim){
		if(isEmpty(delim)) {
			delim = "[ ;]";
		} 
		
		Map<String, String> res = new HashMap<String, String>();
		if(isEmpty(value)) return res;
		
		
		String[] kvs = value.split(delim);
		for(String kv : kvs){
			kv = kv.trim();
			if(kv.equals("")) continue;
			String[] bb = kv.split("=");
			String k="",v="";
			if(bb.length > 0){
				k = bb[0].trim();
			}
			if(bb.length > 1){
				v = bb[1].trim();
			}
			res.put(k, v);
		}
		return res;
	}

}
