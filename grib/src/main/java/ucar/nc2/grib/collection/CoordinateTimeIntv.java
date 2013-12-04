package ucar.nc2.grib.collection;

import ucar.arr.Coordinate;
import ucar.arr.CoordinateBuilder;
import ucar.arr.CoordinateBuilderImpl;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.grib2.Grib2Record;
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
public class CoordinateTimeIntv implements Coordinate, Comparable<CoordinateTimeIntv> {
  private final CalendarDate runtime;
  private final List<TimeCoord.Tinv> timeIntervals;
  //private final List<Coordinate> subdivide; // who needs this ??

  public CoordinateTimeIntv(CalendarDate runtime, List<TimeCoord.Tinv> timeIntervals, List<Coordinate> subdivide) {
    this.runtime = runtime;
    this.timeIntervals = Collections.unmodifiableList(timeIntervals);
    //this.subdivide = (subdivide == null) ? null :  Collections.unmodifiableList(subdivide);
  }

  //public TimeCoord.Tinv extract(Grib2Record gr) {
  //  return extractOffset(gr);
  //}

  public int compareTo(CoordinateTimeIntv o) {
    return runtime.compareTo(o.runtime);
  }

  public CalendarDate getRuntime() {
    return runtime;
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

  static public class Builder extends CoordinateBuilderImpl {
    private final CalendarDate refDate;
    private final Grib2Customizer cust;
    private final CalendarPeriod timeUnit;

    public Builder(Object runtime, Grib2Customizer cust, CalendarPeriod timeUnit) {
      super(runtime);
      this.refDate = (CalendarDate) runtime;
      this.cust = cust;
      this.timeUnit = timeUnit;
    }

    @Override
    public CoordinateBuilder makeBuilder(Object val) {
      CoordinateBuilder result =  new Builder(val, cust, timeUnit);
      result.chainTo(nestedBuilder);
      return result;
    }

    @Override
    protected Object extract(Grib2Record gr) {
      TimeCoord.TinvDate tinvd = cust.getForecastTimeInterval(gr);
      TimeCoord.Tinv tinv = tinvd.convertReferenceDate(refDate, timeUnit);
      return tinv;
    }

    @Override
   protected Coordinate makeCoordinate(List<Object> values, List<Coordinate> subdivide) {
      List<TimeCoord.Tinv> offsetSorted = new ArrayList<>(values.size());
      for (Object val : values) offsetSorted.add( (TimeCoord.Tinv) val);
      Collections.sort(offsetSorted);
      return new CoordinateTimeIntv(refDate, offsetSorted, subdivide);
    }
  }

}
