package ucar.nc2.grib.collection;

import ucar.arr.Coordinate;
import ucar.arr.CoordinateBuilder;
import ucar.arr.CoordinateBuilderImpl;
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
  private final int code;
  private final List<TimeCoord.Tinv> timeIntervals;

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
    return 0;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Type getType() {
    return Type.timeIntv;
  }

  @Override
  public String getUnit() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
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
    info.format("Time Interval offsets:%n");
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
  static public class Builder extends CoordinateBuilderImpl {
    private final Grib2Customizer cust;
    private final int code;
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
