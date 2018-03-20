/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.ncss.validation;

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
			.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.varparams}")
			.addConstraintViolation();
			isValid = false;
		}
								
		return isValid;
	}
	
}
