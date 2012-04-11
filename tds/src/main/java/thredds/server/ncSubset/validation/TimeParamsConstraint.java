package thredds.server.ncSubset.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;


/**
 * 
 * Validation constraint annotation for the time params in a ncss request
 * 
 * @author mhermida
 *
 */
@Target({TYPE, METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy=TimeParamsValidator.class)
@Documented
public @interface TimeParamsConstraint {
	
	String message() default "{thredds.server.ncSubset.validation.ncsstimeparamsvalidator}";
	
	Class<?>[] groups() default {};
	
	Class<? extends Payload>[] payload() default {};

}
