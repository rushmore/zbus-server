package io.zbus.examples.rpc.biz;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class User{
	private String name;
	private String password;
	private int age;
	private String item; 
	private List<String> roles;
	private Map<String, Object> attrs = new HashMap<String, Object>(); 
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	} 
	public String getItem() {
		return item;
	}
	public void setItem(String item) {
		this.item = item;
	}
	public List<String> getRoles() {
		return roles;
	}
	public void setRoles(List<String> roles) {
		this.roles = roles;
	}
	
	public Map<String, Object> getAttrs() {
		return attrs;
	}
	
	public void setAttrs(Map<String, Object> attrs) {
		this.attrs = attrs;
	}
	@Override
	public String toString() {
		return "User [name=" + name + ", password=" + password + ", age=" + age + ", item=" + item + ", roles=" + roles
				+ ", attrs=" + attrs + "]";
	}
	
	
}