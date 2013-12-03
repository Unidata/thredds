package ucar.nc2.grib.collection;

import net.jcip.annotations.Immutable;
import ucar.arr.Coordinate;
import ucar.arr.CoordinateBuilder;
import ucar.arr.CoordinateBuilderImpl;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.Indent;

import java.util.*;

/**
 * Grib runtime coordinate
 *
 * @author caron
 * @since 11/24/13
 */
@Immutable
public class CoordinateRuntime implements Coordinate {
  final List<CalendarDate> runtimeSorted;
  final List<Coordinate> times;

  public CoordinateRuntime(List<CalendarDate> runtimeSorted, List<Coordinate> times) {
    this.runtimeSorted = Collections.unmodifiableList(runtimeSorted);
    this.times = (times == null) ? null : Collections.unmodifiableList(times);
  }

  public List<CalendarDate> getRuntimesSorted() {
    return runtimeSorted;
  }

  public int getSize() {
    return runtimeSorted.size();
  }

  public Type getType() {
    return Type.runtime;
  }

  @Override
  public String getUnit() {
    return null;
  }

  public int getCode() {
    return 0;
  }

  public List<? extends Object> getValues() {
    return runtimeSorted;
  }

  static public CalendarDate extractRunDate(Grib2Record gr) {
    return gr.getReferenceDate();
  }

  public CalendarDate extract(Grib2Record gr) {
    return extractRunDate(gr);
  }

  @Override
  public void showInfo(Formatter info, Indent indent) {
    if (times == null) {
      info.format("%s %20s:", indent, "RunTimes");
      for (CalendarDate cd : runtimeSorted)
        info.format("%s, ", cd);
      info.format("%n");

    } else {
      info.format("%s %20s    Offsets %n", indent, "RunTime");
      int runIdx = 0;
      for (CalendarDate cd : runtimeSorted) {
        Coordinate time = times.get(runIdx); // LOOK sort
        info.format("%s %20s    ", indent, cd);
        for (Object val : time.getValues())
          info.format(" %3s,", val);
        info.format("%n");
        runIdx++;
      }
    }
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Run Times:%n");
    for (CalendarDate cd : runtimeSorted)
      info.format("   %s%n", cd);
  }

  public static class Builder extends CoordinateBuilderImpl {

    public Builder(Object val) {
      super(val);
    }

    @Override
    public CoordinateBuilder makeBuilder(Object val) {
      CoordinateBuilder result =  new Builder(val);
      result.chainTo(nestedBuilder);
      return result;
    }

    @Override
    protected Object extract(Grib2Record gr) {
      return extractRunDate(gr);
    }

  protected Coordinate makeCoordinate(List<Object> values, List<Coordinate> subdivide) {
    List<CalendarDate> runtimeSorted = new ArrayList<>(values.size());
     for (Object val : values) runtimeSorted.add( (CalendarDate) val);
     Collections.sort(runtimeSorted);
     return new CoordinateRuntime(runtimeSorted, subdivide);
   }
  }


}
