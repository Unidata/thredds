package thredds.server.ncSubset.validation;

import thredds.server.ncSubset.params.GridDataRequestParamsBean;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Describe
 *
 * @author caron
 * @since 10/9/13
 */
public class GridDataRequestValidator implements ConstraintValidator<GridDataRequestConstraint, GridDataRequestParamsBean> {

	/* (non-Javadoc)
	 * @see javax.validation.ConstraintValidator#initialize(java.lang.annotation.Annotation)
	 */
	@Override
	public void initialize(GridDataRequestConstraint arg0) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.validation.ConstraintValidator#isValid(java.lang.Object, javax.validation.ConstraintValidatorContext)
	 */
	@Override
	public boolean isValid(GridDataRequestParamsBean params, ConstraintValidatorContext constraintValidatorContext) {

		constraintValidatorContext.disableDefaultConstraintViolation();
		boolean isValid =true;

		// If one is not null, they all have to exist
		if( params.getNorth() != null || params.getSouth() != null ||  params.getEast() != null || params.getWest() != null){

			if( !params.hasLatLonBB()){
				isValid = false;
				constraintValidatorContext
				.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.wrong_bbox}")
				.addConstraintViolation();
			}

		}

		//If one is not null, they all have to
		if( params.getMaxx() != null || params.getMinx() != null ||  params.getMaxy() != null || params.getMiny() != null){
      if( !params.hasProjectionBB()){
				isValid = false;
				constraintValidatorContext
				.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.wrong_bbox}")
				.addConstraintViolation();
			}

		}

		return isValid;
	}

}