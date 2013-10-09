package thredds.server.ncSubset.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Describe
 *
 * @author caron
 * @since 10/9/13
 */
@Target({TYPE, METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy=GridDataRequestValidator.class)
@Documented
public @interface GridDataRequestConstraint {

	String message() default "{thredds.server.ncSubset.validation.wrong_bbox}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}