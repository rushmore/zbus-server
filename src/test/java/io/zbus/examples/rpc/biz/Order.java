package io.zbus.examples.rpc.biz;

import java.util.Arrays;
import java.util.List;


public class Order{ 
	private byte[] data;
	private List<String> item;

	public List<String> getItem() {
		return item;
	}

	public void setItem(List<String> item) {
		this.item = item;
	}
	

	
	
	@Override
	public String toString() {
		return "Order [data=" + Arrays.toString(data) + ", item=" + item + "]";
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
	
	

}