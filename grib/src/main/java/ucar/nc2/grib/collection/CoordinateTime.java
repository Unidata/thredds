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
public class CoordinateTime implements Coordinate {
  private final List<Integer> offsetSorted;
  private final int code;                    // pdsFirst.getTimeUnit()
  private String name = "time";
  private CalendarPeriod timeUnit;
  //private CalendarDate refDate;
  private String periodName;

  public CoordinateTime(List<Integer> offsetSorted, int code) {
    this.offsetSorted = Collections.unmodifiableList(offsetSorted);
    this.code = code;
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
  public Object getValue(int idx) {
    return offsetSorted.get(idx);
  }

  @Override
  public int getSize() {
    return offsetSorted.size();
  }

  public Type getType() {
    return Type.time;
  }

  public CalendarPeriod getPeriod() {
    return timeUnit;
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

  public void setTimeUnit(CalendarPeriod timeUnit) {
    this.timeUnit = timeUnit;
    CalendarPeriod.Field cf = timeUnit.getField();
    if (cf == CalendarPeriod.Field.Month || cf == CalendarPeriod.Field.Year)
      this.periodName = "calendar "+ cf.toString();
    else
      this.periodName = timeUnit.getField().toString();
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

  public int getCode() {
    return code;
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

  static public class Builder extends CoordinateBuilderImpl<Grib2Record>  {
    int code;  // pdsFirst.getTimeUnit()

    public Builder(int code) {
      this.code = code;
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
      return new CoordinateTime(offsetSorted, code);
    }
  }

}
