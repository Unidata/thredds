package thredds.server.ncSubset.validation;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class VarParamsValidator implements ConstraintValidator<VarParamConstraint, List<String>> {

	@Override
	public void initialize(VarParamConstraint constraintAnnotation) {

		
	}

	@Override
	public boolean isValid(List<String> vars, ConstraintValidatorContext constraintValidatorContext) {

		boolean isValid = true;
		
		if( vars == null || vars.isEmpty() ){
			constraintValidatorContext
			.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.ncssvarparamsvalidator}")
			.addConstraintViolation();
			isValid = false;
		}
								
		return isValid;
	}
	
	

}
