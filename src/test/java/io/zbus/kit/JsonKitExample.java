package io.zbus.kit;

public class JsonKitExample {
	public static void main(String[] args) {
		String json = "select:[display_name,id],where:{id:1}";
		json = JsonKit.fixJson(json);
		System.out.println(json);
	}
}
