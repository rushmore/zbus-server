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
package io.zbus.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;

public class FileUtil { 
	
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
			classLoader = FileUtil.class.getClassLoader();
		}
		try {
			if (classLoader != null) {
				URL url = classLoader.getResource(resource);
				if (url == null) {
					System.out.println("Can not find resource:" + resource);
					return null;
				}
				if (url.toString().startsWith("jar:file:")) {
					return FileUtil.class.getResourceAsStream(resource.startsWith("/") ? resource : "/" + resource);
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

	public static String loadFileContent(String resource) {
		InputStream in = FileUtil.class.getClassLoader().getResourceAsStream(resource);
		if (in == null)
			return "";

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
		} catch (IOException e) {
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
