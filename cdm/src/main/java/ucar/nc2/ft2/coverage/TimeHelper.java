/* Copyright */
package ucar.nc2.ft2.coverage;

import ucar.nc2.Attribute;
import ucar.nc2.constants.CF;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.NamedAnything;
import ucar.nc2.util.NamedObject;
import ucar.unidata.util.Format;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for Time coordinates
 *
 * @author caron
 * @since 7/11/2015
 */
public class TimeHelper {
  final Calendar cal;
  final CalendarDateUnit dateUnit;
  final CalendarDate runDate;
  final double duration;

  public TimeHelper(String units, List<Attribute> attributes) {
    this.cal = getCalendarFromAttribute(attributes);
    this.dateUnit = CalendarDateUnit.withCalendar(cal, units); // this will throw exception on failure
    this.runDate = dateUnit.getBaseCalendarDate();
    this.duration = dateUnit.getTimeUnit().getValueInMillisecs();
  }

  public double convert(CalendarDate date) {
    long msecs = date.getDifferenceInMsecs(runDate);
    return msecs / duration;
  }

  public List<NamedObject> getCoordValueNames(CoverageCoordAxis1D axis) {
    axis.getValues(); // read in if needed
    List<NamedObject> result = new ArrayList<>();
    for (int i = 0; i < axis.getNcoords(); i++) {
      String valName = "";
      double value;
      switch (axis.getSpacing()) {
        case regular:
        case irregularPoint:
          value = axis.getCoord(i);
          //valName = Format.d(value, 3);
          result.add(new NamedAnything(makeDate(value), axis.getAxisType().toString()));
          break;

        case contiguousInterval:
        case discontiguousInterval:
          valName = Format.d(axis.getCoordEdge1(i), 3) + "," + Format.d(axis.getCoordEdge2(i), 3);
          result.add(new NamedAnything(valName, valName + " " + axis.getUnits()));
          break;

      }
    }

    return result;
  }

  public CalendarDate makeDate(double value) {
    return dateUnit.makeCalendarDate(value);
  }

  public CalendarDateRange getDateRange(double startValue, double endValue) {
    CalendarDate start = makeDate( startValue);
    CalendarDate end = makeDate( endValue);
    return CalendarDateRange.of(start, end);
  }

  public static ucar.nc2.time.Calendar getCalendarFromAttribute(List<Attribute> atts) {
    Attribute cal = null;
    for (Attribute att : atts) {
      if (att.getName().equalsIgnoreCase(CF.CALENDAR))
       cal = att;
    }
    if (cal == null) return null;
    String s = cal.getStringValue();
    return ucar.nc2.time.Calendar.get(s);
  }
}
