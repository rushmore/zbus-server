package io.zbus.rpc.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
 
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping { 
	String value() default ""; //Alias of path
	String path() default "";
	String[] method() default { };
	boolean exclude() default false; 
	boolean docEnabled() default true;
}
