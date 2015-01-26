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
  private final long[] runtimes;
  final CalendarDate firstDate;
  final CalendarPeriod timeUnit;
  final String periodName;
  private String name = "reftime";   // yeah yeah, not final, bugger off

  public CoordinateRuntime(List<Long> runtimeSorted, CalendarPeriod timeUnit) {
    this.runtimes = new long[runtimeSorted.size()];
    int idx = 0;
    for (long val : runtimeSorted)
      this.runtimes[idx++] = val;

    this.firstDate = CalendarDate.of(runtimeSorted.get(0));
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

  /* public long[] getRuntimesSorted() {
    return runtimes;
  }  */

  public CalendarDate getRuntimeDate(int idx) {
    return CalendarDate.of(runtimes[idx]);
  }

  public long getRuntime(int idx) {
    return runtimes[idx];
  }

  /**
   * Get offsets from firstDate, in units of timeUnit
   * @return for each runtime, a list of values from firstdate
   */
  public List<Double> getOffsetsInTimeUnits() {
    double start = firstDate.getMillis();

    List<Double> result = new ArrayList<>(runtimes.length);
    for (int idx=0; idx<runtimes.length; idx++) {
      double runtime = (double) getRuntime(idx);
      double msecs = (runtime - start);
      result.add(msecs / timeUnit.getValueInMillisecs());
    }
    return result;
  }

  @Override
  public int getSize() {
    return runtimes.length;
  }

  @Override
  public Type getType() {
    return Type.runtime;
  }

  @Override
  public int estMemorySize() {
    return 616 + getSize() * (48);
  }

  @Override
  public String getUnit() {
    return periodName+" since "+firstDate.toString();
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {  // LOOK need to get this in the constructor to make the name final
    if (!this.name.equals("reftime")) throw new IllegalStateException("Cant modify");
    this.name = name;
  }

  @Override
  public int getCode() { return 0; }

  public CalendarDate getFirstDate() {
    return firstDate;
  }

  public CalendarDate getLastDate() {
    return getRuntimeDate(getSize() - 1);
  }

  @Override
  public List<? extends Object> getValues() {
    List<Long> result = new ArrayList<>(runtimes.length);
    for (long val : runtimes) result.add(val);
    return result;
  }

  @Override
  public int getIndex(Object val) {
    return Arrays.binarySearch(runtimes, (Long) val);
  }

  @Override
  public Object getValue(int idx) {
    return runtimes[idx];
  }

  @Override
  public void showInfo(Formatter info, Indent indent) {
    info.format("%s%s:", indent, getType());
    for (int idx=0; idx<getSize(); idx++)
      info.format(" %s,", getRuntimeDate(idx));
    info.format(" (%d) %n", runtimes.length);
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Run Times: %s (%s)%n", getName(), getUnit());
    List<Double> udunits = getOffsetsInTimeUnits();
    int count = 0;
    for (int idx=0; idx<getSize(); idx++)
      info.format("   %s (%f)%n", getRuntimeDate(idx), udunits.get(count++));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CoordinateRuntime that = (CoordinateRuntime) o;

    if (!periodName.equals(that.periodName)) return false;
    if (!Arrays.equals(runtimes, that.runtimes)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(runtimes);
    result = 31 * result + periodName.hashCode();
    return result;
  }

  ///////////////////////////////////////////////////////

  public static class Builder2 extends CoordinateBuilderImpl<Grib2Record> {

    CalendarPeriod timeUnit;

    public Builder2(CalendarPeriod timeUnit) {
      this.timeUnit = timeUnit;
    }

    @Override
    public Object extract(Grib2Record gr) {
      return gr.getReferenceDate().getMillis();
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<Long> runtimeSorted = new ArrayList<>(values.size());
      for (Object val : values)
        runtimeSorted.add((Long) val);
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
      return gr.getReferenceDate().getMillis();
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<Long> runtimeSorted = new ArrayList<>(values.size());
      for (Object val : values) runtimeSorted.add((Long) val);
      Collections.sort(runtimeSorted);
      return new CoordinateRuntime(runtimeSorted, timeUnit);
    }
  }


}
