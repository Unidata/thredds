/* Copyright */
package ucar.nc2.ft2.coverage;

import ucar.ma2.DataType;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.AxisType;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.NamedAnything;
import ucar.nc2.util.NamedObject;
import ucar.unidata.util.Format;

import java.util.ArrayList;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 7/11/2015
 */
public class CoverageCoordAxisTime extends CoverageCoordAxis {

  final CalendarDateUnit dateUnit;
  final CalendarDate runDate;
  final double duration;

  public CoverageCoordAxisTime(String name, String units, String description, DataType dataType, AxisType axisType, AttributeContainer attributes,
                           DependenceType dependenceType, String dependsOn, Spacing spacing, int ncoords, double startValue, double endValue, double resolution,
                           double[] values, ucar.nc2.time.Calendar cal) {
    super(name, units, description, dataType, axisType, attributes, dependenceType, dependsOn, spacing,
                ncoords, startValue, endValue, resolution, values);

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

  public CalendarDateRange getDateRange() {
    CalendarDate start = makeDate( getCoordEdge1(0));
    CalendarDate end = makeDate( getCoordEdgeLast());
    return CalendarDateRange.of(start, end);
  }
}
