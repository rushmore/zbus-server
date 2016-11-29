package io.zbus.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JsonUtil {
	private static Method fastjsonMethod;
	
	static{
		try { 
			Class<?> fastjon = Class.forName("com.alibaba.fastjson.JSON");  
			fastjsonMethod = fastjon.getMethod("toJSONString", Object.class); 
		} catch (Exception e) {  
			//ignore
		}
	}
	
	public static String toJson(Object value) {
		if(fastjsonMethod != null){
			try {
				return (String)fastjsonMethod.invoke(null, value);
			} catch (Exception e) { 
				e.printStackTrace();
			}
		}
		return JsonWriter.toJson(value);
	}
	
	public static void main(String[] args){
		System.out.println(JsonUtil.toJson("xx"));
	}
}



class JsonWriter {
	public static String TimestampPattern = "yyyy-MM-dd HH:mm:ss";
	public static String DatePattern = "yyyy-MM-dd";
	@SuppressWarnings("unchecked")
	public static String toJson(Object value) {
		if(value == null) return "null";
		
		if(value instanceof String)
			return "\"" + escape((String)value) + "\"";
		
		if(value instanceof Double){
			if(((Double)value).isInfinite() || ((Double)value).isNaN())
				return "null";
			else
				return value.toString();
		}
		
		if(value instanceof Float){
			if(((Float)value).isInfinite() || ((Float)value).isNaN())
				return "null";
			else
				return value.toString();
		}
		
		if(value instanceof Number)
			return value.toString();
		
		if(value instanceof Boolean)
			return value.toString();
		
		if (value instanceof java.util.Date) {
			if (value instanceof java.sql.Timestamp)
				return "\"" + new SimpleDateFormat(TimestampPattern).format(value) + "\"";
			if (value instanceof java.sql.Time)
				return "\"" + value.toString() + "\"";
			return "\"" + new SimpleDateFormat(DatePattern).format(value) + "\"";
		}
		
		if(value instanceof Map) {
			return mapToJson((Map<Object,Object>)value);
		}
		
		if(value instanceof List) {
			return listToJson((List<Object>)value);
		}
		
		if (value instanceof Character) {
			return "\"" + escape(value.toString()) + "\"";
		} 
		
		if (value instanceof Object[]) {
			Object[] arr = (Object[])value;
			List<Object> list = new ArrayList<Object>(arr.length);
			for (int i=0; i<arr.length; i++)
				list.add(arr[i]);
			return listToJson(list);
		}
		
		if (value instanceof Enum) {
			return ((Enum<?>)value).toString();
		}
		
		String result = beanToJson(value);  
		if (result != null) return result;
		
		//default to String
		return "\"" + escape(value.toString()) + "\"";
	}
	
	private static String mapToJson(Map<Object,Object> map) {
		if(map == null) return "null";  
		
        StringBuilder sb = new StringBuilder();
        boolean first = true;
		Iterator<?> iter = map.entrySet().iterator(); 
		
        sb.append('{');
		while(iter.hasNext()){
            if(first) first = false;
            else sb.append(',');
            
			Map.Entry<?,?> entry = (Map.Entry<?,?>)iter.next();
			appendKV(String.valueOf(entry.getKey()),entry.getValue(), sb);
		}
        sb.append('}');
		return sb.toString();
	}
	
	private static void appendKV(String key, Object value, StringBuilder sb){
		sb.append('\"');
        if(key == null) sb.append("null");
        else escape(key, sb);
		sb.append('\"').append(':'); 
		sb.append(toJson(value));  
	}

	private static String listToJson(List<Object> list) {
		if(list == null) return "null";
		
        boolean first = true;
        StringBuilder sb = new StringBuilder();
		Iterator<Object> iter = list.iterator();
        
        sb.append('[');
		while(iter.hasNext()){
            if(first) first = false;
            else sb.append(',');
            
			Object value = iter.next();
			if(value == null){
				sb.append("null");
				continue;
			}
			sb.append(toJson(value));
		}
        sb.append(']');
		return sb.toString();
	} 
	
	private static String beanToJson(Object model) {
		Map<Object,Object> map = new HashMap<Object,Object>();
		
		//support for public fields
		Field[] fields = model.getClass().getFields();
		for(Field f : fields){   
			if(Modifier.isStatic(f.getModifiers())) continue;
			String attrName = f.getName();
			Object value = null;
			try { value = f.get(model); } catch (Exception e) {continue;}
			map.put(attrName, value); 
		}
		
		Method[] methods = model.getClass().getMethods();
		for (Method m : methods) {
			String methodName = m.getName();
			int indexOfGet = methodName.indexOf("get");
			if (indexOfGet == 0 && methodName.length() > 3) {	// Only getter
				String attrName = methodName.substring(3);
				if (!attrName.equals("Class")) {				// Ignore Object.getClass()
					Class<?>[] types = m.getParameterTypes();
					if (types.length == 0) {
						try {
							Object value = m.invoke(model);
							map.put(firstCharToLowerCase(attrName), value);
						} catch (Exception e) {
							throw new RuntimeException(e.getMessage(), e);
						}
					}
				}
			}
			else {
               int indexOfIs = methodName.indexOf("is");
               if (indexOfIs == 0 && methodName.length() > 2) {
                  String attrName = methodName.substring(2);
                  Class<?>[] types = m.getParameterTypes();
                  if (types.length == 0) {
                      try {
                          Object value = m.invoke(model);
                          map.put(firstCharToLowerCase(attrName), value);
                      } catch (Exception e) {
                          throw new RuntimeException(e.getMessage(), e);
                      }
                  }
               }
            }
		}
		return mapToJson(map);
	}
	
	private static String firstCharToLowerCase(String str) {
		char firstChar = str.charAt(0);
		if (firstChar >= 'A' && firstChar <= 'Z') {
			char[] arr = str.toCharArray();
			arr[0] += ('a' - 'A');
			return new String(arr);
		}
		return str;
	}
	
	
	private static String escape(String s) {
		if(s == null) return null;
        StringBuilder sb = new StringBuilder();
        escape(s, sb);
        return sb.toString();
    }
	
	private static void escape(String s, StringBuilder sb) {
		for(int i=0; i<s.length(); i++){
			char ch = s.charAt(i);
			switch(ch){
			case '"':
				sb.append("\\\"");
				break;
			case '\\':
				sb.append("\\\\");
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			case '/':
				sb.append("\\/");
				break;
			default:
				if((ch >= '\u0000' && ch <= '\u001F') || (ch >= '\u007F' && ch <= '\u009F') || (ch >= '\u2000' && ch <= '\u20FF')) {
					String str = Integer.toHexString(ch);
					sb.append("\\u");
					for(int k=0; k<4-str.length(); k++) {
						sb.append('0');
					}
					sb.append(str.toUpperCase());
				}
				else{
					sb.append(ch);
				}
			}
		}
	} 
}