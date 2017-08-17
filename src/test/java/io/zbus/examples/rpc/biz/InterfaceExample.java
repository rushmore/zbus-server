package io.zbus.examples.rpc.biz;

import java.util.List;
import java.util.Map;

public interface InterfaceExample{
  
	int getUserScore();
	
	String echo(String string);
	
	String getString(String name); 
	//test of method overloading 
	String getString(String name, int c);
	
	String[] stringArray();
	
	byte[] getBin();
	
	int plus(int a, int b);
	
	MyEnum myEnum(MyEnum e);
	
	User getUser(String name);
	
	Order getOrder();
	
	User[] getUsers();
	
	Object[] objectArray(String id);

	int saveObjectArray(Object[] array);
	
	int saveUserArray(User[] array);
	
	int saveUserList(List<User> array);
	

	Map<String, Object> map(int value1);
	
	List<Map<String, Object>> listMap();
	
	String testEncoding();
	
	Class<?> classTest(Class<?> inClass);
	
	void testTimeout();
	
	void noReturn();
	
	void throwNullPointerException();
	
	void throwException();
	
	void throwUnkownException();
	
	String nullParam(String nullStr);
}