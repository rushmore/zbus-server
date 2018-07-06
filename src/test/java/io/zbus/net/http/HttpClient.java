
package io.zbus.net.http;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import io.zbus.transport.DataHandler;
import io.zbus.transport.ErrorHandler;
import io.zbus.transport.Message;
import io.zbus.transport.http.Http;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpClient {
	private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);
	private final OkHttpClient httpClient;
	private boolean traceEnabled = true;
	
	public HttpClient() {
		OkHttpClient.Builder builder = new OkHttpClient.Builder(); 
		builder.readTimeout(15, TimeUnit.SECONDS);
		this.httpClient = new OkHttpClient();
	}
	
	public void setTraceEnabled(boolean traceEnabled) {
		this.traceEnabled = traceEnabled;
	}

	public <T> List<T> objectArray(Message request, Class<T> type) throws IOException {
		return objectArray(trans(request), type);
	}

	public <T> List<T> objectArray(Request request, Class<T> type) throws IOException {
		String text = string(request);
		return JSON.parseArray(text, type);
	}
	
	public <T> void objectArray(Message request, Class<T> type, final DataHandler<List<T>> handler,
			final ErrorHandler errorHandler) {
		objectArray(trans(request), type, handler, errorHandler);
	}

	public <T> void objectArray(Request request, Class<T> type, final DataHandler<List<T>> handler,
			final ErrorHandler errorHandler) {
		string(request, (str) -> {
			if (handler != null) {
				List<T> t = JSON.parseArray(str, type);
				handler.handle(t);
			}
		}, errorHandler);
	}
	
	public <T> T object(Message request, Class<T> type) throws IOException {
		return object(trans(request), type);
	}

	public <T> T object(Request request, Class<T> type) throws IOException {
		String text = string(request);
		return JSON.parseObject(text, type);
	}
	
	public <T> void object(Message request, Class<T> type, final DataHandler<T> handler,
			final ErrorHandler errorHandler) {
		object(trans(request), type, handler, errorHandler);
	}

	public <T> void object(Request request, Class<T> type, final DataHandler<T> handler,
			final ErrorHandler errorHandler) {
		string(request, (str) -> {
			if (handler != null) {
				T t = JSON.parseObject(str, type);
				handler.handle(t);
			}
		}, errorHandler);
	}
	
	public Object json(Message request) throws IOException {
		return json(trans(request));
	}

	public Object json(Request request) throws IOException {
		String res = string(request);
		return JSON.parse(res);
	}
	
	public void json(Message request, final DataHandler<JSON> handler) {
		json(trans(request), handler);
	}

	public void json(Request request, final DataHandler<JSON> handler) {
		this.json(request, handler, null);
	}
	
	public void json(Message request, final DataHandler<JSON> handler, final ErrorHandler errorHandler) {
		json(trans(request), handler, errorHandler);
	}

	public void json(Request request, final DataHandler<JSON> handler, final ErrorHandler errorHandler) {
		string(request, (str) -> {
			if (handler != null) {
				JSON json = (JSON) JSON.parse(str);
				handler.handle(json);
			}
		}, errorHandler);
	}
	
	public String string(Message request) throws IOException {
		return string(trans(request));
	}

	public String string(Request request) throws IOException {
		String url = "Request(" + request.url().toString() + ")";
		
		final long start = System.currentTimeMillis();  
		final String reqStr = String.format("%s %s", request.method() , request.url().toString()); 
		if(traceEnabled){ 
			String msg = String.format("Request(ID=%d) %s", start, reqStr);
			logger.info(msg);
		} 
		
		Response resp = null;
		try {
			resp = httpClient.newCall(request).execute();
			String body = resp.body().string();
			if (resp.code() != 200) {
				url = "Error: " + body + " " + url;
				RuntimeException exception = new RuntimeException(url);   
				logger.error(exception.getMessage(), exception);
				throw exception;
			}
			
			if(traceEnabled){
				long end = System.currentTimeMillis();
				String bodyLog = body;
				int maxLength = 1024;
				if(bodyLog.length() > maxLength){
					bodyLog = bodyLog.substring(0, maxLength) + " ... [more ignored]";
				}
				String msg = String.format("Resopnse(ID=%d, Time=%dms): %d\n%s", start, (end-start), resp.code(), bodyLog);
				logger.info(msg);
			} 
			return body;

		} catch (IOException e) {
			logger.error(url + " Error: ", e);
			throw e;
		} finally {
			if(resp != null){
				resp.close();
			}
		}
	}

	public void string(Message request, final DataHandler<String> handler,
			final ErrorHandler errorHandler) {
		string(trans(request), handler, errorHandler);
	}
	
	public void string(Request request, final DataHandler<String> handler,
			final ErrorHandler errorHandler) {

		final long start = System.currentTimeMillis();  
		final String reqStr = String.format("%s %s", request.method() , request.url().toString()); 
		if(traceEnabled){ 
			String msg = String.format("AsyncRequest(ID=%d) %s", start, reqStr);
			logger.info(msg);
		} 
		
		httpClient.newCall(request).enqueue(new Callback() {
			@Override
			public void onResponse(Call call, Response resp) throws IOException {
				try {
					String body = resp.body().string();
					if (resp.code() != 200) {
						String url = "Request(" + resp.request().url().toString() + ")";
						url = "Error: " + body + " " + url; 
						RuntimeException exception = new RuntimeException(url);   
						logger.error(exception.getMessage(), exception);

						if (errorHandler != null) {
							errorHandler.handle(exception);
						}
						return;
					}
					
					if(traceEnabled){
						long end = System.currentTimeMillis();
						String bodyLog = body;
						int maxLength = 1024;
						if(bodyLog.length() > maxLength){
							bodyLog = bodyLog.substring(0, maxLength) + " ... [more ignored]";
						}
						String msg = String.format("AsyncResopnse(ID=%d, Time=%dms): %d\n%s", start, (end-start), resp.code(), bodyLog);
						logger.info(msg);
					} 
					
					if (handler != null) {
						try {
							handler.handle(body);
						} catch (Exception e) {
							logger.error(e.getMessage(), e);;
						}
					}
				} finally {
					resp.close();
				}
			}

			@Override
			public void onFailure(Call call, IOException e) {
				logger.error(e.getMessage(), e);
				if (errorHandler != null) {
					errorHandler.handle(e);
				}
			}
		});
	}

	public void send(Request request) {
		send(request, null);
	}

	public void send(Request request, final ErrorHandler errorHandler) {
		final long start = System.currentTimeMillis();  
		final String reqStr = String.format("%s %s", request.method() , request.url().toString()); 
		if(traceEnabled){ 
			String msg = String.format("AsyncRequest(ID=%d) %s", start, reqStr);
			logger.info(msg);
		} 
		
		httpClient.newCall(request).enqueue(new Callback() {
			@Override
			public void onResponse(Call call, Response resp) throws IOException {
				String body = resp.body().string();
				if(traceEnabled){
					long end = System.currentTimeMillis();
					String bodyLog = body;
					int maxLength = 1024;
					if(bodyLog.length() > maxLength){
						bodyLog = bodyLog.substring(0, maxLength) + " ... [more ignored]";
					}
					String msg = String.format("AsyncResopnse(ID=%d, Time=%dms): %d\n%s", start, (end-start), resp.code(), bodyLog);
					logger.info(msg);
				} 
				resp.close();
			}

			@Override
			public void onFailure(Call call, IOException e) {
				logger.error(e.getMessage(), e);
				if (errorHandler != null) {
					errorHandler.handle(e);
				}
			}
		});
	} 
	
	
	public void send(Message request) {
		send(trans(request));
	}
	
	public void send(Message request, final ErrorHandler errorHandler) {
		send(trans(request), errorHandler);
	}
	
	static Request trans(Message msg){
		Builder builder = new Builder();
		HttpUrl url = HttpUrl.parse(msg.getUrl());
		builder.url(url);
		for(Entry<String, String> e : msg.getHeaders().entrySet()){
			builder.addHeader(e.getKey(), e.getValue());
		}
		if("GET".equalsIgnoreCase(msg.getMethod())){
			builder.get();
		} else if("POST".equalsIgnoreCase(msg.getMethod())){ 
			builder.post(RequestBody.create(null, Http.body(msg)));
		} else if("PUT".equalsIgnoreCase(msg.getMethod())){ 
			builder.put(RequestBody.create(null, Http.body(msg)));
		} else if("DELETE".equalsIgnoreCase(msg.getMethod())){ 
			builder.delete(); 
		} 
		return builder.build();
	}
}
