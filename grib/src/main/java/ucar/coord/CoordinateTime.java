package ucar.coord;

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.GribUtils;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
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

  public CoordinateTime(int code, CalendarPeriod timeUnit, CalendarDate refDate, List<Integer> offsetSorted) {
    super(code, timeUnit, refDate);
    this.offsetSorted = Collections.unmodifiableList(offsetSorted);
  }

  /* CoordinateTime(CoordinateTime org, int offset) {
    super(org.getCode(), org.getTimeUnit(), org.getRefDate());
    List<Integer> vals = new ArrayList<>(org.getSize());
    for (int orgVal : org.getOffsetSorted()) vals.add(orgVal+offset);
    this.offsetSorted = Collections.unmodifiableList(vals);
  } */

  /* static public Integer extractOffset(Grib2Record gr) {
    Grib2Pds pds = gr.getPDS();
    return pds.getForecastTime();
  }

  public Integer extract(Grib2Record gr) {
    return extractOffset(gr);
  } */

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

  /* public List<CalendarDate> makeCalendarDates(ucar.nc2.time.Calendar cal, CalendarDate refDate) {
    CalendarDateUnit cdu = CalendarDateUnit.withCalendar(cal, periodName+" since "+ refDate.toString());
    List<CalendarDate> result = new ArrayList<>(getSize());
    for (int val : getOffsetSorted())
      result.add(cdu.makeCalendarDate(val));
    return result;
  } */

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
    info.format("Time offsets: (%s) ref=%s %n", getUnit(), getRefDate());
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

  ////////////////////////////////////////////////

  public CoordinateTime createBestTimeCoordinate(List<Double> runOffsets) {
    Set<Integer> values = new HashSet<>();
    for (double runOffset : runOffsets) {
      for (Integer val : getOffsetSorted())
        values.add((int) (runOffset + val)); // LOOK possible roundoff
    }

    List<Integer> offsetSorted = new ArrayList<>(values.size());
    for (Object val : values) offsetSorted.add( (Integer) val);
    Collections.sort(offsetSorted);
    return new CoordinateTime(getCode(), getTimeUnit(), getRefDate(), offsetSorted);
  }

  /**
   * calculate which runtime to use, based on missing
   * @param runOffsets for each runtime, the offset from base time
   * @param coordBest  best time coordinate, from convertBestTimeCoordinate
   * @param twot       variable missing array
   * @return           for each time in coordBest, which runtime to use, as 1-based index into runtime runOffsets (0 = missing)
   */
  public int[] makeTime2RuntimeMap(List<Double> runOffsets, CoordinateTime coordBest, CoordinateTwoTimer twot) {
    int[] result = new int[ coordBest.getSize()];

    Map<Integer, Integer> map = new HashMap<>();  // lookup coord val to index
    int count = 0;
    for (Integer val : coordBest.getOffsetSorted()) map.put(val, count++);

    int runIdx = 0;
    for (double runOffset : runOffsets) {
      int timeIdx = 0;
      for (Integer val : getOffsetSorted()) {
        if (twot == null || twot.getCount(runIdx, timeIdx) > 0) { // skip missing
          Integer bestVal = (int) (runOffset + val);
          Integer bestValIdx = map.get(bestVal);
          if (bestValIdx == null) throw new IllegalStateException();
          result[bestValIdx] = runIdx+1; // use this partition; later ones override; 1-based so 0 = missing
        }

        timeIdx++;
      }
      runIdx++;
    }
    return result;
  }

  ////////////////////////////////////////////

  /* @Override
  public CoordinateBuilder makeBuilder() {
    return new Builder(code);
  } */

  static public class Builder2 extends CoordinateBuilderImpl<Grib2Record>  {
    private final int code;  // pdsFirst.getTimeUnit()
    private final CalendarPeriod timeUnit;
    private final CalendarDate refDate;

    public Builder2(int code, CalendarPeriod timeUnit, CalendarDate refDate) {
      this.code = code;
      this.timeUnit = timeUnit;
      this.refDate = refDate;
    }

    @Override
    public Object extract(Grib2Record gr) {
      Grib2Pds pds = gr.getPDS();
      int offset = pds.getForecastTime();
      int tuInRecord = pds.getTimeUnit();
      if (tuInRecord == code) {
        return offset;

      } else {
        CalendarPeriod period = Grib2Utils.getCalendarPeriod(tuInRecord);
        CalendarDate validDate = refDate.add(period.multiply(offset));
        int newOffset = TimeCoord.getOffset(refDate, validDate, timeUnit); // offset in correct time unit
        return newOffset;
      }
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<Integer> offsetSorted = new ArrayList<>(values.size());
      for (Object val : values) offsetSorted.add( (Integer) val);
      Collections.sort(offsetSorted);
      return new CoordinateTime(code, timeUnit, refDate, offsetSorted);
    }
  }

  static public class Builder1 extends CoordinateBuilderImpl<Grib1Record>  {
    int code;  // pdsFirst.getTimeUnit()
    CalendarPeriod timeUnit;
    CalendarDate refDate;

    public Builder1(int code, CalendarPeriod timeUnit, CalendarDate refDate) {
      this.code = code;
      this.timeUnit = timeUnit;
      this.refDate = refDate;
    }

    @Override
    public Object extract(Grib1Record gr) {
      Grib1SectionProductDefinition pds = gr.getPDSsection();
      int offset = pds.getTimeValue1();
      int tuInRecord = pds.getTimeUnit();
      if (tuInRecord == code) {
        return offset;

      } else {
        CalendarPeriod period = GribUtils.getCalendarPeriod(tuInRecord);
        CalendarDate validDate = refDate.add( period.multiply(offset));
        int newOffset = TimeCoord.getOffset(refDate, validDate, timeUnit);
        return newOffset;
      }

    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<Integer> offsetSorted = new ArrayList<>(values.size());
      for (Object val : values) offsetSorted.add( (Integer) val);
      Collections.sort(offsetSorted);
      return new CoordinateTime(code, timeUnit, refDate, offsetSorted);
    }
  }



}
