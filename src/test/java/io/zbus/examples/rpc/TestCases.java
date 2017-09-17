package io.zbus.examples.rpc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;

import io.zbus.examples.rpc.biz.InterfaceExample;
import io.zbus.examples.rpc.biz.MyEnum;
import io.zbus.examples.rpc.biz.Order;
import io.zbus.examples.rpc.biz.User;
import io.zbus.examples.rpc.biz.generic.GenericMethod;
import io.zbus.examples.rpc.biz.inheritance.SubServiceInterface1;
import io.zbus.examples.rpc.biz.inheritance.SubServiceInterface2;
import io.zbus.rpc.Request;
import io.zbus.rpc.Response;
import io.zbus.rpc.RpcInvoker;

public class TestCases {
	public static User getUser(String name) {
		User user = new User();
		user.setName(name);
		user.setPassword("password" + System.currentTimeMillis());
		user.setAge(new Random().nextInt(100));
		user.setItem("item_1");
		user.setRoles(Arrays.asList("admin", "common"));
		return user;
	}
	
	
	public static void testRpcInvoker(RpcInvoker rpc){
		Object res = rpc.invokeSync(String.class, "echo", "test");
		System.out.println(res);

		//overlapping test
		res = rpc.invokeSync(String.class, "getString", new Class[]{String.class, int.class}, "version2", 2); 
		System.out.println(res);
		
		res = rpc.invokeSync("getOrder"); 
		System.out.println(res);
		
		Request req;
		req = new Request().method("getOrder");
		Response resp = rpc.invokeSync(req);
		System.out.println(resp.getResult());
		
		Order order = rpc.invokeSync(Order.class, req);
		System.out.println(order); 
		
	}
	
	public static void testGenericMethod(GenericMethod biz) throws Exception{ 
		biz.test(10);
	}
	
	public static void testInheritGeneric1(SubServiceInterface1 biz) {
		biz.save(10);
	}
	
	public static void testInheritGeneric2(SubServiceInterface2 biz) {
		biz.save("hello world");
	}
	 
	public static void testDynamicProxy(InterfaceExample biz) throws Exception{ 
		
		List<Map<String, Object>> list = biz.listMap();
		System.out.println(list);
		
		Object[] res = biz.objectArray("xzx");
		for (Object obj : res) {
			System.out.println(obj);
		}

		Object[] array = new Object[] { getUser("rushmore"), "hong", true, 1,
				String.class };
		
		String encoding = biz.testEncoding();
		Assert.assertEquals(encoding, "中文");
		
		int saved = biz.saveObjectArray(array);
		System.out.println(saved);
		 
		Class<?> ret = biz.classTest(String.class);
		System.out.println(ret);
		
		User[] users = new User[]{ getUser("rushmore"),  getUser("rushmore2")};
		biz.saveUserArray(users);
		
		biz.saveUserList(Arrays.asList(users));
		
		Object[] objects = biz.getUsers();
		for(Object obj : objects){
			System.out.println(obj);
		} 
		
		MyEnum e = biz.myEnum(MyEnum.Monday);
		System.out.println(e);
		
		Order order = biz.getOrder();
		System.out.println(order);
		
		byte[] bin = biz.getBin();
		System.out.println(bin);
		
		try{
			biz.throwNullPointerException();
		} catch (NullPointerException ex) {
			System.out.println(ex);
		}
		
		try{
			biz.throwUnkownException();
		} catch (Exception ex) {
			System.out.println(ex);
		}
		try{
			String nullStr = biz.nullParam(null);
			System.out.println(nullStr);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
	}
}
