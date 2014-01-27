package ucar.nc2.grib.collection;

import net.jcip.annotations.Immutable;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.sparr.Coordinate;
import ucar.sparr.CoordinateBuilderImpl;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.Indent;

import java.util.*;

/**
 * Time coordinates that are not intervals.
 *
 * @author caron
 * @since 11/24/13
 */
@Immutable
public class CoordinateTime extends CoordinateTimeAbstract implements Coordinate {
  private final List<Integer> offsetSorted;

  public CoordinateTime(int code, CalendarPeriod timeUnit, List<Integer> offsetSorted) {
    super(code, timeUnit);
    this.offsetSorted = Collections.unmodifiableList(offsetSorted);
  }

  CoordinateTime(CoordinateTime org, int offset) {
    super(org.getCode(), org.getTimeUnit());
    List<Integer> vals = new ArrayList<>(org.getSize());
    for (int orgVal : org.getOffsetSorted()) vals.add(orgVal+offset);
    this.offsetSorted = Collections.unmodifiableList(vals);
  }

  static public Integer extractOffset(Grib2Record gr) {
    Grib2Pds pds = gr.getPDS();
    return pds.getForecastTime();
  }

  public Integer extract(Grib2Record gr) {
    return extractOffset(gr);
  }

  public List<Integer> getOffsetSorted() {
    return offsetSorted;
  }

  @Override
  public List<? extends Object> getValues() {
    return offsetSorted;
  }

  @Override
  public int getIndex(Object val) {
    return offsetSorted.indexOf(val);
  }

  @Override
  public Object getValue(int idx) {
    if (idx < 0 || idx >= offsetSorted.size())
      return null;
    return offsetSorted.get(idx);
  }

  @Override
  public int getSize() {
    return offsetSorted.size();
  }

  public Type getType() {
    return Type.time;
  }

  public List<CalendarDate> makeCalendarDates(ucar.nc2.time.Calendar cal, CalendarDate refDate) {
    CalendarDateUnit cdu = CalendarDateUnit.withCalendar(cal, periodName+" since "+ refDate.toString());
    List<CalendarDate> result = new ArrayList<>(getSize());
    for (int val : getOffsetSorted())
      result.add(cdu.makeCalendarDate(val));
    return result;
  }

  public CalendarDateRange makeCalendarDateRange(ucar.nc2.time.Calendar cal, CalendarDate refDate) {
    CalendarDateUnit cdu = CalendarDateUnit.withCalendar(cal, periodName + " since " + refDate.toString());
    CalendarDate start = cdu.makeCalendarDate(offsetSorted.get(0));
    CalendarDate end = cdu.makeCalendarDate(offsetSorted.get(getSize()-1));
    return CalendarDateRange.of(start, end);
  }

  @Override
  public void showInfo(Formatter info, Indent indent) {
    info.format("%s%s:", indent, getType());
     for (Integer cd : offsetSorted)
       info.format(" %3d,", cd);
    info.format(" (%d) %n", offsetSorted.size());
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Time offsets: (%s) %n", getUnit());
     for (Integer cd : offsetSorted)
       info.format("   %3d%n", cd);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CoordinateTime that = (CoordinateTime) o;

    if (code != that.code) return false;
    if (!offsetSorted.equals(that.offsetSorted)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = offsetSorted.hashCode();
    result = 31 * result + code;
    return result;
  }

  ////////////////////////////////////////////

  /* @Override
  public CoordinateBuilder makeBuilder() {
    return new Builder(code);
  } */

  static public class Builder extends CoordinateBuilderImpl<Grib2Record>  {
    int code;  // pdsFirst.getTimeUnit()
    CalendarPeriod timeUnit;

    public Builder(int code, CalendarPeriod timeUnit) {
      this.code = code;
      this.timeUnit = timeUnit;
    }

    @Override
    public Object extract(Grib2Record gr) {
      return extractOffset(gr);
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<Integer> offsetSorted = new ArrayList<>(values.size());
      for (Object val : values) offsetSorted.add( (Integer) val);
      Collections.sort(offsetSorted);
      return new CoordinateTime(code, timeUnit, offsetSorted);
    }
  }

}
