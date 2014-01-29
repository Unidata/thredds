package ucar.nc2.grib.collection;

import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.sparr.Coordinate;
import ucar.sparr.CoordinateBuilderImpl;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.Indent;
import ucar.sparr.CoordinateTwoTimer;

import java.util.*;

/**
 * Time coordinates that are intervals.
 *
 * @author John
 * @since 11/28/13
 */
public class CoordinateTimeIntv extends CoordinateTimeAbstract implements Coordinate {
  private final List<TimeCoord.Tinv> timeIntervals;

  //public CoordinateTimeIntv(Grib2Customizer cust, CalendarPeriod timeUnit, int code, List<TimeCoord.Tinv> timeIntervals) {
  public CoordinateTimeIntv(int code, CalendarPeriod timeUnit, CalendarDate refDate, List<TimeCoord.Tinv> timeIntervals) {
    super(code, timeUnit, refDate);
    this.timeIntervals = Collections.unmodifiableList(timeIntervals);
  }

  CoordinateTimeIntv(CoordinateTimeIntv org, int offset) {
    super(org.getCode(), org.getTimeUnit(), org.getRefDate());
    List<TimeCoord.Tinv> vals = new ArrayList<>(org.getSize());
    for (TimeCoord.Tinv orgVal : org.getTimeIntervals()) vals.add(new TimeCoord.Tinv(orgVal.getBounds1()+offset, orgVal.getBounds2()+offset));
    this.timeIntervals = Collections.unmodifiableList(vals);
  }

  public List<TimeCoord.Tinv> getTimeIntervals() {
    return timeIntervals;
  }

  @Override
  public List<? extends Object> getValues() {
    return timeIntervals;
  }

  @Override
  public Object getValue(int idx) {
    if (idx >= timeIntervals.size())
      System.out.println("HEY");
    return timeIntervals.get(idx);
  }

  @Override
  public int getIndex(Object val) {
    return timeIntervals.indexOf(val);
  }

  public int getSize() {
    return timeIntervals.size();
  }
  @Override
  public Type getType() {
    return Type.timeIntv;
  }

  /* public void setRefDate(CalendarDate refDate) {
    this.refDate = refDate;
  } */

  public String getTimeIntervalName() {

    // are they the same length ?
    int firstValue = -1;
    boolean same = true;
    for (TimeCoord.Tinv tinv : timeIntervals) {
      int value = (tinv.getBounds2() - tinv.getBounds1());
      if (firstValue < 0) firstValue = value;
      else if (value != firstValue) same = false;
    }

    if (same) {
      firstValue = (int) (firstValue * getTimeUnitScale());
      return firstValue + "_" + timeUnit.getField().toString();
    } else {
      return "Mixed_intervals";
    }
  }

  public List<CalendarDate> makeCalendarDates(ucar.nc2.time.Calendar cal, CalendarDate refDate) {
    CalendarDateUnit cdu = CalendarDateUnit.withCalendar(cal, periodName+" since "+ refDate.toString());
    List<CalendarDate> result = new ArrayList<>(getSize());
    for (TimeCoord.Tinv val : getTimeIntervals())
      result.add(cdu.makeCalendarDate(val.getBounds2())); // use the upper bound - same as iosp uses for coord
    return result;
  }

  public CalendarDateRange makeCalendarDateRange(ucar.nc2.time.Calendar cal, CalendarDate refDate) {
    CalendarDateUnit cdu = CalendarDateUnit.withCalendar(cal, periodName + " since " + refDate.toString());
    CalendarDate start = cdu.makeCalendarDate(timeIntervals.get(0).getBounds2());
    CalendarDate end = cdu.makeCalendarDate(timeIntervals.get(getSize()-1).getBounds2());
    return CalendarDateRange.of(start, end);
  }

  @Override
  public void showInfo(Formatter info, Indent indent) {
    info.format("%s%s:", indent, getType());
     for (TimeCoord.Tinv cd : timeIntervals)
       info.format(" %s,", cd);
    info.format(" (%d) %n", timeIntervals.size());
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Time Interval offsets: (%s) ref=%s%n", getUnit(), getRefDate());
    for (TimeCoord.Tinv cd : timeIntervals)
      info.format("   (%3d - %3d)  %d%n", cd.getBounds1(), cd.getBounds2(), cd.getBounds2() - cd.getBounds1());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CoordinateTimeIntv that = (CoordinateTimeIntv) o;

    if (code != that.code) return false;
    if (!timeIntervals.equals(that.timeIntervals)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = code;
    result = 31 * result + timeIntervals.hashCode();
    return result;
  }

  ////////////////////////////////////////

    // make the union of all the offsets from base date
  public CoordinateTimeIntv createBestTimeCoordinate(List<Double> runOffsets) {
    Set<TimeCoord.Tinv> values = new HashSet<>();
    for (double runOffset : runOffsets) {
      for (TimeCoord.Tinv val : getTimeIntervals())
        values.add( val.offset(runOffset)); // LOOK possible roundoff
    }

    List<TimeCoord.Tinv> offsetSorted = new ArrayList<>(values.size());
    for (Object val : values) offsetSorted.add( (TimeCoord.Tinv) val);
    Collections.sort(offsetSorted);
    return new CoordinateTimeIntv(getCode(), getTimeUnit(), refDate, offsetSorted);
  }

  protected int[] makeTime2RuntimeMap(List<Double> runOffsets, CoordinateTimeIntv coordBest, CoordinateTwoTimer twot) {
    int[] result = new int[ coordBest.getSize()];

    Map<TimeCoord.Tinv, Integer> map = new HashMap<>();  // lookup coord val to index
    int count = 0;
    for (TimeCoord.Tinv val : coordBest.getTimeIntervals()) map.put(val, count++);

    int runIdx = 0;
    for (double runOffset : runOffsets) {
      int timeIdx = 0;
      for (TimeCoord.Tinv val : getTimeIntervals()) {
        if (twot.getCount(runIdx, timeIdx) > 0) { // skip missing;
          TimeCoord.Tinv bestVal = val.offset(runOffset);
          Integer bestValIdx = map.get(bestVal);
          if (bestValIdx == null) throw new IllegalStateException();
          result[bestValIdx] = runIdx+1; // use this partition; later ones override; one based so 0 = missing
        }

        timeIdx++;
      }
      runIdx++;
    }
    return result;
  }

  ///////////////////////////////////////////////////////////

 /* @Override
  public CoordinateBuilder makeBuilder() {
    return new Builder(cust, timeUnit, code);
  }  */

  static public class Builder extends CoordinateBuilderImpl<Grib2Record> {
    private final Grib2Customizer cust;
    private final int code;                  // pdsFirst.getTimeUnit()
    private final CalendarPeriod timeUnit;
    private final CalendarDate refDate;

    public Builder(Grib2Customizer cust, int code, CalendarPeriod timeUnit, CalendarDate refDate) {
      this.cust = cust;
      this.code = code;
      this.timeUnit = timeUnit;
      this.refDate = refDate;
    }

    @Override
    public Object extract(Grib2Record gr) {
      CalendarDate refDate2 =  gr.getReferenceDate();
      if (!refDate.equals(refDate2)) {
        System.out.printf("ReferenceDate %s != %s%n", refDate2, refDate);
       // LOOK ??
      }

      CalendarPeriod timeUnitUse = timeUnit;
      Grib2Pds pds = gr.getPDS();
      int tu2 = pds.getTimeUnit();
      if (tu2 != code) {
        System.out.printf("Time unit diff %d != %d%n", tu2, code);
        int unit = cust.convertTimeUnit(tu2);
        timeUnitUse = Grib2Utils.getCalendarPeriod(unit);
      }

      TimeCoord.TinvDate tinvd = cust.getForecastTimeInterval(gr);
      TimeCoord.Tinv tinv = tinvd.convertReferenceDate(refDate2, timeUnitUse);
      return tinv;
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<TimeCoord.Tinv> offsetSorted = new ArrayList<>(values.size());
      for (Object val : values) offsetSorted.add( (TimeCoord.Tinv) val);
      Collections.sort(offsetSorted);

      return new CoordinateTimeIntv(code, timeUnit, refDate, offsetSorted);
    }
  }

}
