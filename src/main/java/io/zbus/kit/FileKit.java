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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.transport.Message;
import io.zbus.transport.http.Http;
import io.zbus.transport.http.Http.FileForm;
import io.zbus.transport.http.Http.FileUpload;

public class FileKit {
	private final Map<String, String> cache = new ConcurrentHashMap<String, String>();
	private boolean enableCache = true;

	public static FileKit INSTANCE = new FileKit();

	public FileKit() {

	}

	public FileKit(boolean cached) {
		this.enableCache = cached;
	}

	public void setCache(boolean value) {
		enableCache = value;
	}

	public static InputStream inputStream(String resource) {
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
					return new FileInputStream(new File(resource));
				}
				if (url.toString().startsWith("jar:file:")) {
					return FileKit.class.getResourceAsStream(resource.startsWith("/") ? resource : "/" + resource);
				} else {
					return new FileInputStream(new File(url.toURI()));
				}
			}
		} catch (Exception e) {
			// ignore
		}
		return null;
	}

	public String loadFile(String resource) throws IOException {
		if (enableCache && cache.containsKey(resource)) {
			return cache.get(resource);
		}

		InputStream in = FileKit.class.getClassLoader().getResourceAsStream(resource);
		if (in == null) {
			throw new IOException(resource + " not found");
		}

		Writer writer = new StringWriter();
		char[] buffer = new char[1024];
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			int n;
			while ((n = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, n);
			}
		} catch (UnsupportedEncodingException e) {
			// ignore
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				// ignore
			}
		}
		String content = writer.toString();
		cache.put(resource, content);
		return content;
	}

	public String loadFile(String resource, Map<String, Object> model) throws IOException {
		String template = loadFile(resource);
		if (model == null)
			return template;

		for (Entry<String, Object> e : model.entrySet()) {
			String key = e.getKey();
			Object val = e.getValue();

			key = "{{" + key + "}}";
			if (val == null) {
				val = "";
			}
			template = template.replace(key, val.toString());
		}

		return template;
	}

	public byte[] loadFileBytes(String resource) throws IOException {
		InputStream in = inputStream(resource);
		if (in == null)
			throw new FileNotFoundException("File(" + resource + ") Not Found");

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] data = new byte[1024];
		try {
			int nRead;
			while ((nRead = in.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}
		} finally {
			try {
				buffer.close();
				in.close();
			} catch (IOException e) {
				// ignore
			}
		}
		return buffer.toByteArray();
	} 
	
	public Message loadResource(String resource) {
		return loadResource(resource, new HashMap<>());
	}
	
	public Message loadResource(String resource, Map<String, Object> model) {
		Message res = new Message();
		
		UrlInfo info = HttpKit.parseUrl(resource); 
		String contentType = HttpKit.contentType(resource);
		if(contentType == null) {
			contentType = "application/octet-stream";
		}
		
		res.setHeader(Http.CONTENT_TYPE, contentType);   
		res.setStatus(200); 
		try {
			byte[] data = loadFileBytes(resource);
			res.setBody(data);
		} catch (IOException e) {
			res.setStatus(404);
			res.setBody(info.urlPath + " Not Found");
		}  
		return res;
	}

	public static void deleteFile(File file) {
		if (file.exists()) {
			if (file.isDirectory()) {
				File[] files = file.listFiles();
				if (files == null)
					return;
				for (int i = 0; i < files.length; i++) {
					deleteFile(files[i]);
				}
			}
			file.delete();
		}
	}

	public static File classBaseDir() {
		return classBaseDir(FileKit.class);
	}

	public static File classBaseDir(Class<?> clazz) {
		if (clazz == null) {
			clazz = FileKit.class;
		}
		return new File(clazz.getProtectionDomain().getCodeSource().getLocation().getPath());
	}
	
	
	public static void saveUploadedFile(Message req, String basePath) {
		FileForm fileForm = (FileForm)req.getBody();
		if (fileForm == null) {
			throw new IllegalArgumentException("upload body is null");
		} 

		BufferedOutputStream bos = null;
		FileOutputStream fos = null;
		
		File base = new File(basePath);
		if(!base.exists()) {
			base.mkdirs();
		}

		for (String key : fileForm.files.keySet()) {
			List<FileUpload> fileUploads = fileForm.files.get(key);
			for (FileUpload fileUpload : fileUploads) {
				try {
					File file = new File(basePath, fileUpload.fileName); 
					
					fos = new FileOutputStream(file);
					bos = new BufferedOutputStream(fos);
					bos.write(fileUpload.data); 
					
				} catch (Exception e) {
					throw new RuntimeException(e.getMessage(), e.getCause()); 
				} finally {
					try {
						if (bos != null) {
							bos.close();
						}
						if (fos != null) {
							fos.close();
						}
					} catch (Exception e) {
						throw new RuntimeException(e.getMessage(), e.getCause()); 
					}
				}
			}
		} 
	}

}
