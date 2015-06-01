/* Copyright */
package ucar.nc2.ft2.coverage.grid;

import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.NamedAnything;
import ucar.nc2.util.NamedObject;
import ucar.unidata.util.Format;

import java.util.ArrayList;
import java.util.List;

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

  @Override
  public List<NamedObject> getCoordValueNames() {
    getValues();
    List<NamedObject> result = new ArrayList<>();
    for (int i = 0; i < getNcoords(); i++) {
      String valName = "";
      double value;
      switch (getSpacing()) {
        case regular:
        case irregularPoint:
          value = getCoord(i);
          valName = Format.d(value, 3);
          result.add(new NamedAnything(valName, makeDate(value).toString()));
          break;

        case contiguousInterval:
        case discontiguousInterval:
          valName = Format.d(getCoordEdge1(i), 3) + "," + Format.d(getCoordEdge2(i), 3);
          result.add(new NamedAnything(valName, valName + " " + getUnits()));
          break;

      }
    }

    return result;
  }

  public CalendarDate makeDate(double value) {
    return dateUnit.makeCalendarDate(value);
  }

}
