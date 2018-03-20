/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.ncss.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import thredds.server.ncss.params.NcssPointParamsBean;

/**
 * Check ncss point params
 *  1) does it have a stn parameter?
 *
 */
public class PointHorizSubsetTypeValidator implements ConstraintValidator<PointHorizSubsetTypeConstraint, NcssPointParamsBean> {

	/* (non-Javadoc)
	 * @see javax.validation.ConstraintValidator#initialize(java.lang.annotation.Annotation)
	 */
	@Override
	public void initialize(PointHorizSubsetTypeConstraint arg0) {
		
	}

	/* (non-Javadoc)
	 * @see javax.validation.ConstraintValidator#isValid(java.lang.Object, javax.validation.ConstraintValidatorContext)
	 */
	@Override
	public boolean isValid(NcssPointParamsBean params, ConstraintValidatorContext constraintValidatorContext) {

		constraintValidatorContext.disableDefaultConstraintViolation();
		boolean isValid =true;

    boolean isStnRequest =  params.hasLatLonPoint() && params.hasStations();
    boolean isPointRequest =  params.hasLatLonPoint() && !params.hasStations();

		// if no stn param is provided ignore all the others, it must be a point request
		// if stn == all --> all stations		
		if( !isStnRequest && !isPointRequest ){
				isValid = false;
				constraintValidatorContext
				.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.lat_or_lon_missing}")
				.addConstraintViolation();				
		}
		
		/* if( params.getSubset() != null && !params.getSubset().equals("stns") && !params.getSubset().equals("all") && !params.getSubset().equals("bb")  ){
			isValid = false;
			constraintValidatorContext
			.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.subsettypeerror}")
			.addConstraintViolation();			
		}		
		
		if( params.getSubset() != null && params.getSubset().equals("stns") && params.getStns() == null ){
			isValid = false;
			constraintValidatorContext
			.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.subsettypeerror.no_stns_param}")
			.addConstraintViolation();			
		}
		
		if( params.getSubset() != null && params.getSubset().equals("bb") && (params.getNorth()  == null || params.getSouth() == null || params.getEast() == null || params.getWest() == null  )){
			isValid = false;
			constraintValidatorContext
			.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.subsettypeerror.no_bounding_box}")
			.addConstraintViolation();			
		}		*/
		
		
		return isValid;
	}

}
