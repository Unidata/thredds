/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.ncss.validation;

import thredds.server.ncss.params.NcssGridParamsBean;
import ucar.nc2.time.CalendarDate;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator for ncss GridRequest
 * 1) if has either latitude or longitude, has both
 * 2) if has any of latlon bb, has all. north > south, east > west
 * 3) if has any of projection bb, has all. min < max
 * <p>
 * Messages in WEB-INF/classes/ValidationMessages.properties
 *
 * @author caron
 * @since 10/9/13
 */
public class NcssGridRequestValidator implements ConstraintValidator<NcssGridRequestConstraint, NcssGridParamsBean> {

  /* (non-Javadoc)
   * @see javax.validation.ConstraintValidator#initialize(java.lang.annotation.Annotation)
   */
  @Override
  public void initialize(NcssGridRequestConstraint arg0) {
    // TODO Auto-generated method stub
  }

  /*
   * since none of these are required, can only do consistency checks
   * @see "http://www.unidata.ucar.edu/software/thredds/current/tds/reference/NetcdfSubsetServiceReference.html"
   */
  @Override
  public boolean isValid(NcssGridParamsBean params, ConstraintValidatorContext constraintValidatorContext) {

    constraintValidatorContext.disableDefaultConstraintViolation();
    boolean isValid = true;

    // lat/lon point
    if (params.getLatitude() != null || params.getLongitude() != null) {
      if (!params.hasLatLonPoint()) {
        isValid = false;
        constraintValidatorContext.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.lat_or_lon_missing}").addConstraintViolation();
      }
    }

    // lat/lon bb
    if (params.getNorth() != null || params.getSouth() != null || params.getEast() != null || params.getWest() != null) {
      if (!params.hasLatLonBB()) {
        isValid = false;
        constraintValidatorContext.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.wrong_bbox}").addConstraintViolation();
      }

      if (params.getNorth() < params.getSouth()) {
        isValid = false;
        constraintValidatorContext.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.north_south}").addConstraintViolation();
      }
      if (params.getEast() < params.getWest()) {
        isValid = false;
        constraintValidatorContext.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.east_west}").addConstraintViolation();
      }
    }

    // proj bb
    if (params.getMaxx() != null || params.getMinx() != null || params.getMaxy() != null || params.getMiny() != null) {
      if (!params.hasProjectionBB()) {
        isValid = false;
        constraintValidatorContext.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.wrong_pbox}").addConstraintViolation();
      }

      if (params.getMaxx() < params.getMinx()) {
        isValid = false;
        constraintValidatorContext.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.rangex}").addConstraintViolation();
      }
      if (params.getMaxy() < params.getMiny()) {
        isValid = false;
        constraintValidatorContext.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.rangey}").addConstraintViolation();
      }
    }

    // runtime: latest, all, or time
    if (params.getRuntime() != null) {
      if ("latest".equalsIgnoreCase(params.getRuntime())) {
        params.setLatestRuntime(true);

      } else if ("all".equalsIgnoreCase(params.getRuntime())) {
        params.setAllRuntime(true);

      } else {
        CalendarDate cd = TimeParamsValidator.validateISOString(params.getRuntime(), "{thredds.server.ncSubset.validation.param.runtime}", constraintValidatorContext);
        if (cd != null)
          params.setRuntimeDate(cd);
      }
    }

    // timeOffset: first or double
    if (params.getTimeOffset() != null) {
      if ("first".equalsIgnoreCase(params.getTimeOffset())) {
        params.setFirstTimeOffset(true);

      } else {
        try {
          double val = Double.parseDouble(params.getTimeOffset());
          params.setTimeOffsetVal(val);
        } catch (NumberFormatException e) {
          constraintValidatorContext.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.param.time_offset}").addConstraintViolation();
        }
      }
    }

    return isValid;
  }
}