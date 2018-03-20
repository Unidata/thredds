/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.ncss.validation;

import java.text.ParseException;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import thredds.server.ncss.params.NcssParamsBean;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;

/**
 * Time params validator for a ncss request.
 * 1) validate time parameter if exists
 * 2) validate time_window parameter if exists
 * 3) if any of time range exists, validate its a valid time range
 * <p/>
 * Create the CalendarDate, CalendarDateRange with default Calendar.
 *
 * @author mhermida
 */
public class TimeParamsValidator implements ConstraintValidator<TimeParamsConstraint, NcssParamsBean> {

  public void initialize(TimeParamsConstraint constraintAnnotation) {
  }

  public boolean isValid(NcssParamsBean params, ConstraintValidatorContext constraintValidatorContext) {
    constraintValidatorContext.disableDefaultConstraintViolation();

    // time point with optional window
    String time = params.getTime();
    String time_window = params.getTime_window();
    if (time != null) {
      if ("all".equals(time)) return true;

      CalendarDate cd = validateISOString(time, "{thredds.server.ncSubset.validation.param.time}", constraintValidatorContext);
      if (cd != null)
        params.setDate(cd);

      if (time_window != null) {  // LOOK
        try {
          params.setTimeWindow(new TimeDuration(time_window));
        } catch (ParseException pe) {
          constraintValidatorContext.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.param.time_window}").addConstraintViolation();
          return false;
        }
      }
      return cd != null;
    }

    // time range
    String time_start = params.getTime_start();
    String time_end = params.getTime_end();
    String time_duration = params.getTime_duration();

    // all null are valid parameters
    if (time_start == null && time_end == null && time_duration == null) return true;

    // has 2 of 3
    if (!hasValidDateRange(time_start, time_end, time_duration)) {
      constraintValidatorContext.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.time.2of3}").addConstraintViolation();
      return false;
    }

    //check the formats
    boolean isValid = true;
    if (time_start != null) {
      isValid = (null != validateISOString(time_start, "{thredds.server.ncSubset.validation.param.time_start}", constraintValidatorContext));
    }

    if (time_end != null) {
      isValid &= (null != validateISOString(time_end, "{thredds.server.ncSubset.validation.param.time_end}", constraintValidatorContext));
    }

    if (time_duration != null) {
      try {
        new TimeDuration(time_duration);

      } catch (ParseException pe) {
        isValid = false;
        constraintValidatorContext.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.param.time_duration}").addConstraintViolation();
      }
    }

    //check time_start < time_end
    if (isValid && time_start != null && time_end != null) {
      CalendarDate start = isoString2Date(time_start);
      CalendarDate end = isoString2Date(time_end);

      if (start.isAfter(end)) {
        isValid = false;
        constraintValidatorContext.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.time.start_gt_end}").addConstraintViolation();
      }
    }

    // make calendar range with default calendar
    if (isValid) try {
        Calendar cal = Calendar.getDefault();
        DateRange dr = new DateRange(new DateType(time_start, null, null, cal), new DateType(time_end, null, null, cal), new TimeDuration(time_duration), null);
        CalendarDateRange cdr = CalendarDateRange.of(dr.getStart().getCalendarDate(), dr.getEnd().getCalendarDate());
        params.setDateRange(cdr);

      } catch (ParseException pe) {
        isValid = false;
        constraintValidatorContext.buildConstraintViolationWithTemplate("{thredds.server.ncSubset.validation.timeparams}").addConstraintViolation();
      }

    return isValid;
  }

  /**
   * Determine if a valid date range was specified
   *
   * @return true if there is a valid date range, false if not.
   */
  private boolean hasValidDateRange(String time_start, String time_end, String time_duration) {
    // no range
    if ((null == time_start) && (null == time_end) && (null == time_duration))
      return false;

    if ((null != time_start) && (null != time_end))
      return true;

    if ((null != time_start) && (null != time_duration))
      return true;

    if ((null != time_end) && (null != time_duration))
      return true;

    // misformed range
    // errs.append("Must have 2 of 3 parameters: time_start, time_end, time_duration\n");
    return false;
  }

  public static CalendarDate validateISOString(String isoString, String msg, ConstraintValidatorContext constraintValidatorContext) {
    try {
      return isoString2Date(isoString);
    } catch (IllegalArgumentException iea) {
      constraintValidatorContext.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
    }
    return null;
  }

  private static CalendarDate isoString2Date(String isoString) {
    if ("present".equalsIgnoreCase(isoString)) return CalendarDate.present();
    return CalendarDateFormatter.isoStringToCalendarDate(Calendar.getDefault(), isoString);
  }
}
