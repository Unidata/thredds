package ucar.nc2.grib.collection;

import net.jcip.annotations.Immutable;
import ucar.sparr.Coordinate;
import ucar.sparr.CoordinateBuilderImpl;
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
  final CalendarDate firstDate;

  public CoordinateRuntime(List<CalendarDate> runtimeSorted) {
    this.runtimeSorted = Collections.unmodifiableList(runtimeSorted);
    firstDate = runtimeSorted.get(0);
  }

  public List<CalendarDate> getRuntimesSorted() {
    return runtimeSorted;
  }

  public List<Integer> getRuntimesUdunits() {
    List<Integer> result = new ArrayList<>(runtimeSorted.size());
    for (CalendarDate cd : runtimeSorted) {
      cd.getDifferenceInMsecs(firstDate);
    }
    return result;
  }

  public int getSize() {
    return runtimeSorted.size();
  }

  public Type getType() {
    return Type.runtime;
  }

  @Override
  public String getUnit() {
    return "secs since "+firstDate.toString();
  }

  @Override
  public String getName() {
    return "reftime";
  }

  public int getCode() {
    return 0;
  }

  public CalendarDate getFirstDate() {
    return firstDate;
  }

  public List<? extends Object> getValues() {
    return runtimeSorted;
  }

  public Object getValue(int idx) {
    return runtimeSorted.get(idx);
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

  public static class Builder extends CoordinateBuilderImpl<Grib2Record> {

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
