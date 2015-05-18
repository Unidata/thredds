/* Copyright */
package ucar.nc2.ft2.coverage.grid;

import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.units.TimeUnit;

/**
 * GridCoordAxis with type=AxisType.Time
 *
 * @author caron
 * @since 5/9/2015
 */
public class GridCoordAxisTime extends GridCoordAxis {
  final CalendarDateUnit dateUnit;
  final CalendarDate runDate;
  final double duration;

  protected GridCoordAxisTime(GridCoordAxis from, Calendar cal) {
    super(from);
    this.dateUnit = CalendarDateUnit.withCalendar(cal, getUnits()); // this will throw exception on failure
    this.runDate = dateUnit.getBaseCalendarDate();
    this.duration = dateUnit.getTimeUnit().getValueInMillisecs();
  }

  public double convert(CalendarDate date) {
    long msecs = date.getDifferenceInMsecs(runDate);
    return Math.round(msecs / duration);
  }

}
