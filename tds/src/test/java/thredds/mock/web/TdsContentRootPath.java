package thredds.mock.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets a tds.content.root.path for the tests using MockTdsContextLoader 
 * To override default value just annotate the tests with this annotation
 * 
 * ONLY FOR TESTS THAT USE THE MockTdsContextLoader !!!!!
 *  
 * @author mhermida
 *
 */
@Target(ElementType.TYPE ) //Tests use the MockTdsContextLoader to load a TdsContext for all tests in class so this annotation is applied to class level
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface TdsContentRootPath {
	/**
	 * Our content root path 
	 */
	String path() default "../../content";
	
}
