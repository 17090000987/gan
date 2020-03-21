package gan.core.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME) 
public @interface JsonAnnotation {
	
	public Class<?> listItem() default Void.class;
	
	public String	jsonKey() default "";
	
	public String	getJsonKeyMethod() default "";
	
	public boolean	checkFieldNull() default false;
	
	/**
	 * 当json中没有对应的字段key，直接用json，创建对象
	 * 用于对象中的包含对象，但json中没有包含子json
	 * @return
	 */
	public boolean	buildItem() default false;
}
