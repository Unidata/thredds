/* Copyright */
package ucar.nc2.ft.cover;

import ucar.nc2.time.CalendarDate;

/**
 * Describe
 *
 * @author caron
 * @since 4/7/2015
 */
public interface AxisTime extends Axis<CalendarDate> {

  @Override
  CalendarDate getStart();

  @Override
  CalendarDate getEnd();

  @Override
  CalendarDate getResolution();

}
