package thredds.server.ncss.validation;

import thredds.server.ncss.format.SupportedFormat;
import thredds.server.ncss.format.SupportedOperation;
import thredds.server.ncss.params.NcssParamsBean;
import ucar.nc2.NetcdfFileWriter;

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
	 * @see "http://www.unidata.ucar.edu/software/thredds/current/tds/reference/NetcdfSubsetServiceReference.html"
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