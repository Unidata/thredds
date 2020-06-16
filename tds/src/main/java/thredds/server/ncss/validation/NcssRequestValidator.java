/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.ncss.validation;

import thredds.server.ncss.params.NcssParamsBean;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Describe
 *
 * @author caron
 * @since 10/9/13
 */
public class NcssRequestValidator implements ConstraintValidator<NcssRequestConstraint, NcssParamsBean> {

	/* (non-Javadoc)
	 * @see javax.validation.ConstraintValidator#initialize(java.lang.annotation.Annotation)
	 */
	@Override
	public void initialize(NcssRequestConstraint arg0) {
		// TODO Auto-generated method stub

	}

	/*
	 * since none of these are required, can only do consistency checks
	 * @see "https://www.unidata.ucar.edu/software/tds/current/reference/NetcdfSubsetServiceReference.html"
	 */
	@Override
	public boolean isValid(NcssParamsBean params, ConstraintValidatorContext constraintValidatorContext) {

		constraintValidatorContext.disableDefaultConstraintViolation();
    boolean isValid =true;

    // lat/lon point
		if( params.getLatitude() != null || params.getLongitude() != null) {
			if ( !params.hasLatLonPoint()){
				isValid = false;
				constraintValidatorContext
				.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.lat_or_lon_missing}")
				.addConstraintViolation();
			}
		}

		// lat/lon bb
		if( params.getNorth() != null || params.getSouth() != null ||  params.getEast() != null || params.getWest() != null){
			if( !params.hasLatLonBB()){
				isValid = false;
				constraintValidatorContext
				.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.wrong_bbox}")
				.addConstraintViolation();
			}

		}

		// proj bb
		if( params.getMaxx() != null || params.getMinx() != null ||  params.getMaxy() != null || params.getMiny() != null){
      if( !params.hasProjectionBB()){
				isValid = false;
				constraintValidatorContext
				.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.wrong_pbox}")
				.addConstraintViolation();
			}
		}

		return isValid;
	}

}