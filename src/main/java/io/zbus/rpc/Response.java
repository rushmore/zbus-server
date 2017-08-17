package io.zbus.rpc;

public class Response {  
	private Object result;
	private Object error; //if not null, means error happened, otherwise result is OK
	
	public Object getResult() {
		return result;
	}
	
	public void setResult(Object result) {
		this.result = result;
	}
	
	public Object getError(){
		return this.error;
	}
	
	public void setError(Object error){
		this.error = error;
	}  
}