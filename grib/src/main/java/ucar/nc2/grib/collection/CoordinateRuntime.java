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

  public CoordinateRuntime(List<CalendarDate> runtimeSorted) {
    this.runtimeSorted = Collections.unmodifiableList(runtimeSorted);
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
    info.format("%s%s %20s:", indent, getType(), "RunTimes");
    for (CalendarDate cd : runtimeSorted)
      info.format(" %s,", cd);
    info.format(" (%d) %n", runtimeSorted.size());
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Run Times:%n");
    for (CalendarDate cd : runtimeSorted)
      info.format("   %s%n", cd);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CoordinateRuntime that = (CoordinateRuntime) o;

    if (!runtimeSorted.equals(that.runtimeSorted)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return runtimeSorted.hashCode();
  }

  ///////////////////////////////////////////////////////

  public static class Builder extends CoordinateBuilderImpl {

    @Override
    public Object extract(Grib2Record gr) {
      return extractRunDate(gr);
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<CalendarDate> runtimeSorted = new ArrayList<>(values.size());
      for (Object val : values) runtimeSorted.add((CalendarDate) val);
      Collections.sort(runtimeSorted);
      return new CoordinateRuntime(runtimeSorted);
    }
  }


}
