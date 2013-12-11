package ucar.nc2.grib.collection;

import ucar.sparr.Coordinate;
import ucar.sparr.CoordinateBuilderImpl;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.Indent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

/**
 * Time coordinates that are intervals.
 *
 * @author John
 * @since 11/28/13
 */
public class CoordinateTimeIntv implements Coordinate {
  private final int code;                               // pdsFirst.getTimeUnit()
  private final List<TimeCoord.Tinv> timeIntervals;
  private String name = "time";
  private CalendarPeriod timeUnit;
  //private CalendarDate refDate;
  private String periodName;

  public CoordinateTimeIntv(List<TimeCoord.Tinv> timeIntervals, int code) {
    this.timeIntervals = Collections.unmodifiableList(timeIntervals);
    this.code = code;
  }

  public List<TimeCoord.Tinv> getTimeIntervals() {
    return timeIntervals;
  }

  public List<? extends Object> getValues() {
    return timeIntervals;
  }

  public int getSize() {
    return timeIntervals.size();
  }

  @Override
  public int getCode() {
    return code;
  }

  @Override
  public Type getType() {
    return Type.timeIntv;
  }

  public CalendarPeriod getPeriod() {
    return timeUnit;
  }

  @Override
  public String getUnit() {
    return periodName;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /* public void setRefDate(CalendarDate refDate) {
    this.refDate = refDate;
  } */

  public void setTimeUnit(CalendarPeriod timeUnit) {
    this.timeUnit = timeUnit;
    CalendarPeriod.Field cf = timeUnit.getField();
    if (cf == CalendarPeriod.Field.Month || cf == CalendarPeriod.Field.Year)
      this.periodName = "calendar "+ cf.toString();
    else
      this.periodName = timeUnit.getField().toString();
  }

  public String getTimeIntervalName() {

    // are they the same length ?
    int firstValue = -1;
    boolean same = true;
    for (TimeCoord.Tinv tinv : timeIntervals) {
      int value = (tinv.getBounds2() - tinv.getBounds1());
      if (firstValue < 0) firstValue = value;
      else if (value != firstValue) same = false;
    }

    if (same) {
      firstValue = (int) (firstValue * getTimeUnitScale());
      return firstValue + "_" + timeUnit.getField().toString();
    } else {
      return "Mixed_intervals";
    }
  }

  public double getTimeUnitScale() {
     return timeUnit.getValue();
   }

  @Override
  public void showInfo(Formatter info, Indent indent) {
    info.format("%s%s Tinv:", indent, getType());
     for (TimeCoord.Tinv cd : timeIntervals)
       info.format(" %s,", cd);
    info.format(" (%d) %n", timeIntervals.size());
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Time Interval offsets: (%s) %n", getUnit());
    for (TimeCoord.Tinv cd : timeIntervals)
      info.format("   (%3d - %3d)  %d%n", cd.getBounds1(), cd.getBounds2(), cd.getBounds2() - cd.getBounds1());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CoordinateTimeIntv that = (CoordinateTimeIntv) o;

    if (code != that.code) return false;
    if (!timeIntervals.equals(that.timeIntervals)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = code;
    result = 31 * result + timeIntervals.hashCode();
    return result;
  }

  ///////////////////////////////////////////////////////////
  static public class Builder extends CoordinateBuilderImpl<Grib2Record> {
    private final Grib2Customizer cust;
    private final int code;                  // pdsFirst.getTimeUnit()
    private final CalendarPeriod timeUnit;

    public Builder(Grib2Customizer cust, CalendarPeriod timeUnit, int code) {
      this.cust = cust;
      this.timeUnit = timeUnit;
      this.code = code;
    }


    @Override
    public Object extract(Grib2Record gr) {
      CalendarDate refDate =  gr.getReferenceDate();

      CalendarPeriod timeUnitUse = timeUnit;
      Grib2Pds pds = gr.getPDS();
      int tu2 = pds.getTimeUnit();
      if (tu2 != code) {
        System.out.printf("Time unit diff %d != %d%n", tu2, code);
        int unit = cust.convertTimeUnit(tu2);
        timeUnitUse = Grib2Utils.getCalendarPeriod(unit);
      }

      TimeCoord.TinvDate tinvd = cust.getForecastTimeInterval(gr);
      TimeCoord.Tinv tinv = tinvd.convertReferenceDate(refDate, timeUnitUse);
      return tinv;
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<TimeCoord.Tinv> offsetSorted = new ArrayList<>(values.size());
      for (Object val : values) offsetSorted.add( (TimeCoord.Tinv) val);
      Collections.sort(offsetSorted);

      return new CoordinateTimeIntv( offsetSorted, code);
    }
  }

}
