package ucar.coord;

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.GribUtils;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.grib1.Grib1ParamTime;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.Indent;
import ucar.nc2.util.Misc;

import java.util.*;

/**
 * Time coordinates that are offsets from the reference date (not intervals).
 *
 * @author caron
 * @since 11/24/13
 */
@Immutable
public class CoordinateTime extends CoordinateTimeAbstract implements Coordinate {
  private final List<Integer> offsetSorted;

  public CoordinateTime(int code, CalendarPeriod timeUnit, CalendarDate refDate, List<Integer> offsetSorted, int[] time2runtime) {
    super(code, timeUnit, refDate, time2runtime);
    this.offsetSorted = Collections.unmodifiableList(offsetSorted);
  }

  CoordinateTime(CoordinateTime org, CalendarDate refDate) {
    super(org.code, org.timeUnit, refDate, null);
    this.offsetSorted = org.getOffsetSorted();
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
    return Collections.binarySearch(offsetSorted, (Integer) val);
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

  @Override
  public Type getType() {
    return Type.time;
  }

  @Override
  public int estMemorySize() {
    return 320 + getSize() * (16);
  }

  @Override
  public CalendarDateRange makeCalendarDateRange(ucar.nc2.time.Calendar cal) {
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
    if (time2runtime != null)
      info.format("%stime2runtime: %s", indent, Misc.showInts(time2runtime));
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

  protected CoordinateTimeAbstract makeBestFromComplete(int[] best, int n) {
    List<Integer> offsetSortedBest = new ArrayList<>(offsetSorted.size());
    int[] time2runtimeBest = new int[n];
    int count = 0;
    for (int i=0; i<best.length; i++) {
      int time = best[i];
      if (time >= 0) {
        time2runtimeBest[count] = time;
        offsetSortedBest.add(offsetSorted.get(i));
        count++;
      }
    }

    return new CoordinateTime(code, timeUnit, refDate, offsetSortedBest, time2runtimeBest);
  }


  /* public CoordinateTime makeBestTimeCoordinate(List<Double> runOffsets) {
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

  /*
   * calculate which runtime to use, based on missing
   * @param runOffsets for each runtime, the offset from base time
   * @param coordBest  best time coordinate, from convertBestTimeCoordinate
   * @param twot       variable missing array
   * @return           for each time in coordBest, which runtime to use, as 1-based index into runtime runOffsets (0 = missing)
   *
  public int[] makeTime2RuntimeMap(List<Double> runOffsets, CoordinateTime coordBest, TwoDTimeInventory twot) {
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
  }   */

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
      return new CoordinateTime(code, timeUnit, refDate, offsetSorted, null);
    }
  }

  static public class Builder1 extends CoordinateBuilderImpl<Grib1Record>  {
    final Grib1Customizer cust;
    final int code;  // pdsFirst.getTimeUnit()
    final CalendarPeriod timeUnit;
    final CalendarDate refDate;

    public Builder1(Grib1Customizer cust, int code, CalendarPeriod timeUnit, CalendarDate refDate) {
      this.cust = cust;
      this.code = code;
      this.timeUnit = timeUnit;
      this.refDate = refDate;
    }

    @Override
    public Object extract(Grib1Record gr) {
      Grib1SectionProductDefinition pds = gr.getPDSsection();
      Grib1ParamTime ptime = gr.getParamTime(cust);

      int offset = ptime.getForecastTime();
      int tuInRecord = pds.getTimeUnit();
      if (tuInRecord == code) {
        return offset;

      } else {
        CalendarDate validDate = GribUtils.getValidTime(refDate, tuInRecord, offset);
        int newOffset = TimeCoord.getOffset(refDate, validDate, timeUnit);
        return newOffset;
      }

    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<Integer> offsetSorted = new ArrayList<>(values.size());
      for (Object val : values) offsetSorted.add( (Integer) val);
      Collections.sort(offsetSorted);
      return new CoordinateTime(code, timeUnit, refDate, offsetSorted, null);
    }
  }



}
