package io.zbus.rpc.biz;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


public interface DbInterface { 
	@Select("SELECT * FROM  user WHERE User=#{userId}")
	Map<String, Object> getUser(@Param("userId") String userId);
	
	@Select("SELECT * FROM proc WHERE name = #{name}")
	Map<String, Object> getProc(@Param("name") String name);
	 
	@Select("SELECT * FROM help_category")
	List<Map<String, Object>> helpCate();   
	
	@Select("SELECT name, description FROM help_topic limit 10")
	List<Map<String, Object>> helpTopic();
}
