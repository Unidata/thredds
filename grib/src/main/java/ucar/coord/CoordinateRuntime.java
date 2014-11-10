package ucar.coord;

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.Indent;

import java.util.*;

/**
 * Grib runtime coordinate
 * Effectively Immutable
 * @author caron
 * @since 11/24/13
 */
@Immutable
public class CoordinateRuntime implements Coordinate {
  final List<CalendarDate> runtimeSorted;
  final CalendarDate firstDate;
  final CalendarPeriod timeUnit;
  final String periodName;
  String name = "reftime";

  public CoordinateRuntime(List<CalendarDate> runtimeSorted, CalendarPeriod timeUnit) {
    this.runtimeSorted = Collections.unmodifiableList(runtimeSorted);
    firstDate = runtimeSorted.get(0);
    this.timeUnit = timeUnit == null ? CalendarPeriod.Hour : timeUnit;

    CalendarPeriod.Field cf = this.timeUnit.getField();
    if (cf == CalendarPeriod.Field.Month || cf == CalendarPeriod.Field.Year)
      this.periodName = "calendar "+ cf.toString();
    else
      this.periodName = cf.toString();
  }

  public CalendarPeriod getTimeUnits() {
    return timeUnit;
  }

  public List<CalendarDate> getRuntimesSorted() {
    return runtimeSorted;
  }

  /**
   * Get offsets from firstDate, in units of timeUnit
   * @return for each runtime, a list of values from firstdate
   */
  public List<Double> getOffsetsInTimeUnits() {
    List<Double> result = new ArrayList<>(runtimeSorted.size());
    for (CalendarDate cd : runtimeSorted) {
      double msecs = (double) cd.getDifferenceInMsecs(firstDate);
      result.add(msecs / timeUnit.getValueInMillisecs());
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
    return periodName+" since "+firstDate.toString();
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    if (!this.name.equals("reftime")) throw new IllegalStateException("Cant modify");
    this.name = name;
  }

  @Override
  public int getCode() { return 0; }

  public CalendarDate getFirstDate() {
    return firstDate;
  }

  public CalendarDate getLastDate() {
    return runtimeSorted.get(getSize()-1);
  }

  public CalendarDate getDate(int idx) {
    return runtimeSorted.get(idx);
  }

  @Override
  public List<? extends Object> getValues() {
    return runtimeSorted;
  }

  @Override
  public int getIndex(Object val) {   // LOOK log lookoup - should be a hash
    return Collections.binarySearch(runtimeSorted, (CalendarDate) val);
  }

  @Override
  public Object getValue(int idx) {
    return runtimeSorted.get(idx);
  }

  @Override
  public void showInfo(Formatter info, Indent indent) {
    info.format("%s%s:", indent, getType());
    for (CalendarDate cd : runtimeSorted)
      info.format(" %s,", cd);
    info.format(" (%d) %n", runtimeSorted.size());
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Run Times: %s (%s)%n", getName(), getUnit());
    List<Double> udunits = getOffsetsInTimeUnits();
    int count = 0;
    for (CalendarDate cd : runtimeSorted) {
      info.format("   %s (%f)%n", cd, udunits.get(count++));
    }
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

  public static class Builder2 extends CoordinateBuilderImpl<Grib2Record> {

    CalendarPeriod timeUnit;

    public Builder2(CalendarPeriod timeUnit) {
      this.timeUnit = timeUnit;
    }

    @Override
    public Object extract(Grib2Record gr) {
      return gr.getReferenceDate();
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<CalendarDate> runtimeSorted = new ArrayList<>(values.size());
      for (Object val : values) runtimeSorted.add((CalendarDate) val);
      Collections.sort(runtimeSorted);
      return new CoordinateRuntime(runtimeSorted, timeUnit);
    }
  }

  public static class Builder1 extends CoordinateBuilderImpl<Grib1Record> {
    CalendarPeriod timeUnit;

    public Builder1(CalendarPeriod timeUnit) {
      this.timeUnit = timeUnit;
    }

    @Override
    public Object extract(Grib1Record gr) {
      return gr.getReferenceDate();
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<CalendarDate> runtimeSorted = new ArrayList<>(values.size());
      for (Object val : values) runtimeSorted.add((CalendarDate) val);
      Collections.sort(runtimeSorted);
      return new CoordinateRuntime(runtimeSorted, timeUnit);
    }
  }


}
