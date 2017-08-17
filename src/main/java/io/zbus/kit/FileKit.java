/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.zbus.kit;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class FileKit { 
	
	public static InputStream inputStream(String filePath){
		File file = new File(filePath);
		if(file.exists()){
			try {
				return new FileInputStream(file);
			} catch (FileNotFoundException e) {
				return null;
			}
		} 
		return FileKit.class.getClassLoader().getResourceAsStream(filePath);
	}
	
	public static InputStream loadFile(String resource, Class<?> clazz) {
		ClassLoader classLoader = null;
		try {
			Method method = Thread.class.getMethod("getContextClassLoader");
			classLoader = (ClassLoader) method.invoke(Thread.currentThread());
		} catch (Exception e) {
			System.out.println("loadConfigFile error: ");
			e.printStackTrace();
		}
		if (classLoader == null) {
			classLoader = clazz.getClassLoader();
		}
		try {
			if (classLoader != null) {
				URL url = classLoader.getResource(resource);
				if (url == null) {
					System.out.println("Can not find resource:" + resource);
					return null;
				}
				if (url.toString().startsWith("jar:file:")) {
					return clazz.getResourceAsStream(resource.startsWith("/") ? resource : "/" + resource);
				} else {
					return new FileInputStream(new File(url.toURI()));
				}
			}
		} catch (Exception e) {
			System.out.println("loadConfigFile error: ");
			e.printStackTrace();
		}
		return null;
	}

	public static InputStream loadFile(String resource) {
		ClassLoader classLoader = null;
		try {
			Method method = Thread.class.getMethod("getContextClassLoader");
			classLoader = (ClassLoader) method.invoke(Thread.currentThread());
		} catch (Exception e) {
			System.out.println("loadConfigFile error: ");
			e.printStackTrace();
		}
		if (classLoader == null) {
			classLoader = FileKit.class.getClassLoader();
		}
		try {
			if (classLoader != null) {
				URL url = classLoader.getResource(resource);
				if (url == null) {
					System.out.println("Can not find resource:" + resource);
					return null;
				}
				if (url.toString().startsWith("jar:file:")) {
					return FileKit.class.getResourceAsStream(resource.startsWith("/") ? resource : "/" + resource);
				} else {
					return new FileInputStream(new File(url.toURI()));
				}
			}
		} catch (Exception e) {
			System.out.println("loadConfigFile error: ");
			e.printStackTrace();
		}
		return null;
	}

	public static String loadFileString(String resource) throws IOException {
		InputStream in = FileKit.class.getClassLoader().getResourceAsStream(resource);
		if (in == null) return null;

		Writer writer = new StringWriter();
		char[] buffer = new char[1024];
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			int n;
			while ((n = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, n);
			}
		} catch (UnsupportedEncodingException e) { 
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return writer.toString();
	}
	
	public static String loadTemplate(String resource, Map<String, Object> model) throws IOException {
		String template = loadFileString(resource);
		if(model == null) return template; 
		
		for(Entry<String, Object> e : model.entrySet()){
			String key = e.getKey();
			Object val = e.getValue();
			
			key = "\\{\\{"+key+"\\}\\}";
			if(val instanceof String){
				val = "\"" + val + "\"";
			}
			template = template.replaceAll(key, val.toString()); 
		}
		
		return template;
	}
	
	public static Map<String, Object> parseKeyValuePairs(String params){
		Map<String, Object> model = new HashMap<String, Object>();
		String[] kvs = params.split("&&");
		for(String kv : kvs){
			kv = kv.trim();
			String[] bb = kv.split("=");
			if(bb.length < 2) continue;
			String key = bb[0].trim();
			String val = bb[1].trim();
			model.put(key, val);
		}
		return model;
	}
	
	public static byte[] loadFileBytes(String resource) throws IOException {
		InputStream in = FileKit.class.getClassLoader().getResourceAsStream(resource);
		if (in == null)
			return null;

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[1024];
		try {
			while ((nRead = in.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return buffer.toByteArray();
	}

	public static void deleteFile(File file) {
		if (file.exists()) {
			if (file.isDirectory()) {
				File[] files = file.listFiles();
				if(files == null) return; 
				for (int i = 0; i < files.length; i++) {
					deleteFile(files[i]);
				} 
			} 
			file.delete(); 
		}  
	}
}
