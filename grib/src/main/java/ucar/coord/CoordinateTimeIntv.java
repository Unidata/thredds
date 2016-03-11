/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.coord;

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.GribUtils;
import ucar.nc2.grib.grib1.Grib1ParamTime;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.Counters;
import ucar.nc2.util.Indent;
import ucar.nc2.util.Misc;

import java.util.*;

/**
 * Time coordinates that are intervals.
 *
 * @author John
 * @since 11/28/13
 */
@Immutable
public class CoordinateTimeIntv extends CoordinateTimeAbstract implements Coordinate {
  private final List<TimeCoord.Tinv> timeIntervals;

  public CoordinateTimeIntv(int code, CalendarPeriod timeUnit, CalendarDate refDate, List<TimeCoord.Tinv> timeIntervals, int[] time2runtime) {
    super(code, timeUnit, refDate, time2runtime);
    this.timeIntervals = Collections.unmodifiableList(timeIntervals);
  }

  CoordinateTimeIntv(CoordinateTimeIntv org, CalendarDate refDate) {
    super(org.code, org.timeUnit, refDate, null);
    this.timeIntervals = org.getTimeIntervals();
  }

  public List<TimeCoord.Tinv> getTimeIntervals() {
    return timeIntervals;
  }

  /* @Override
  public int findIndexContaining(Object need) {
    int idx = findSingleHit(need);
    if (idx >= 0) return idx;
    if (idx == -1) return -1;
    // multiple hits = choose closest to the midpoint
    return findClosest(need);
  }

   // return index if only one match, if no matches return -1, if > 1 match return -nhits
  private int findSingleHit(double target) {
    int hits = 0;
    int idxFound = -1;
    for (int i = 0; i < timeIntervals.size(); i++) {
      TimeCoord.Tinv level = timeIntervals.get(i);
      if (contains(target, level)) {
        hits++;
        idxFound = i;
      }
    }
    if (hits == 1) return idxFound;
    if (hits == 0) return -1;
    return -hits;
  }

  // return index of closest value to target
  private int findClosest(double target) {
    double minDiff =  Double.MAX_VALUE;
    int idxFound = -1;
    for (int i = 0; i < timeIntervals.size(); i++) {
      TimeCoord.Tinv level = timeIntervals.get(i);
      double midpoint = (level.getBounds1()+level.getBounds2())/2.0;
      double diff =  Math.abs(midpoint - target);
      if (diff < minDiff) {
        minDiff = diff;
        idxFound = i;
      }
    }
    return idxFound;
  }

  private boolean contains(double target, TimeCoord.Tinv level) {
    if (level.getBounds1() <= target && target <= level.getBounds2()) return true;
    return level.getBounds1() >= target && target >= level.getBounds2();
  } */

  @Override
  public List<? extends Object> getValues() {
    return timeIntervals;
  }

  @Override
  public Object getValue(int idx) {
    return timeIntervals.get(idx);
  }

  @Override
  public int getIndex(Object val) {
    return Collections.binarySearch(timeIntervals, (TimeCoord.Tinv) val);
  }

  @Override
  public int getSize() {
    return timeIntervals.size();
  }

  @Override
  public int estMemorySize() {
    return 616 + getSize() * (24);  // LOOK wrong
  }

  @Override
  public Type getType() {
    return Type.timeIntv;
  }

  /**
   * Check if we all time intervals have the same length.
   * @return time interval name or MIXED_INTERVALS
   */
  public String getTimeIntervalName() {
    // are they the same length ?
    int firstValue = -1;
    for (TimeCoord.Tinv tinv : timeIntervals) {
      int value = (tinv.getBounds2() - tinv.getBounds1());
      if (firstValue < 0) firstValue = value;
      else if (value != firstValue) return MIXED_INTERVALS;
    }

    firstValue = (int) (firstValue * getTimeUnitScale());
    return firstValue + "_" + timeUnit.getField().toString();
  }

  /**
   * Make calendar date range, using the first and last ending bounds
   * @param cal  optional calendar, may be null
   * @return  calendar date range
   */
  @Override
  public CalendarDateRange makeCalendarDateRange(ucar.nc2.time.Calendar cal) {
    CalendarDateUnit cdu = CalendarDateUnit.of(cal, timeUnit.getField(), refDate);
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
    if (time2runtime != null)
      info.format("%stime2runtime: %s", indent, Misc.showInts(time2runtime));
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Time Interval offsets: (%s) ref=%s%n", getUnit(), getRefDate());
    for (TimeCoord.Tinv cd : timeIntervals)
      info.format("   (%3d - %3d)  %d%n", cd.getBounds1(), cd.getBounds2(), cd.getBounds2() - cd.getBounds1());
  }

  @Override
  public Counters calcDistributions() {
    ucar.nc2.util.Counters counters = new Counters();
    counters.add("resol");
    counters.add("intv");

    List<TimeCoord.Tinv> offsets = getTimeIntervals();
    for (int i = 0; i < offsets.size(); i++) {
      int intv = offsets.get(i).getBounds2() - offsets.get(i).getBounds1();
      counters.count("intv", intv);
      if (i < offsets.size() - 1) {
        int resol = offsets.get(i + 1).getBounds1() - offsets.get(i).getBounds1();
        counters.count("resol", resol);
      }
    }

    return counters;
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

  protected CoordinateTimeIntv makeBestFromComplete(int[] best, int n) {
    List<TimeCoord.Tinv> timeIntervalsBest = new ArrayList<>(timeIntervals.size());
    int[] time2runtimeBest = new int[n];
    int count = 0;
    for (int i=0; i<best.length; i++) {
      int time = best[i];
      if (time >= 0) {
        time2runtimeBest[count] = time;
        timeIntervalsBest.add(timeIntervals.get(i));
        count++;
      }
    }

    return new CoordinateTimeIntv(code, timeUnit, refDate, timeIntervalsBest, time2runtimeBest);
  }

  ///////////////////////////////////////////////////////////

  static public class Builder2 extends CoordinateBuilderImpl<Grib2Record> {
    private final Grib2Customizer cust;
    private final int code;                  // pdsFirst.getTimeUnit()
    private final CalendarPeriod timeUnit;
    private final CalendarDate refDate;

    public Builder2(Grib2Customizer cust, int code, CalendarPeriod timeUnit, CalendarDate refDate) {
      this.cust = cust;
      this.code = code;
      this.timeUnit = timeUnit;
      this.refDate = refDate;
    }

    public Builder2(CoordinateTimeIntv from) {
      this.cust = null;
      this.code = from.getCode();
      this.timeUnit = from.getTimeUnit();
      this.refDate = from.getRefDate();
    }

    @Override
    public Object extract(Grib2Record gr) {
      TimeCoord.Tinv tinv;

      Grib2Pds pds = gr.getPDS();
      int tuInRecord = pds.getTimeUnit();
      if (tuInRecord == code) {
        int[] intv = cust.getForecastTimeIntervalOffset(gr);
        tinv = new TimeCoord.Tinv(intv[0], intv[1]);

      } else {
        // int unit = cust.convertTimeUnit(tu2);  // not used
        TimeCoord.TinvDate tinvd = cust.getForecastTimeInterval(gr); // converts to calendar date
        tinv = tinvd.convertReferenceDate(refDate, timeUnit);
      }

      return tinv;
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<TimeCoord.Tinv> offsetSorted = new ArrayList<>(values.size());
      for (Object val : values) offsetSorted.add( (TimeCoord.Tinv) val);
      Collections.sort(offsetSorted);

      return new CoordinateTimeIntv(code, timeUnit, refDate, offsetSorted, null);
    }
  }

  static public class Builder1 extends CoordinateBuilderImpl<Grib1Record> {
    private final Grib1Customizer cust;
    private final int code;                  // pdsFirst.getTimeUnit()
    private final CalendarPeriod timeUnit;
    private final CalendarDate refDate;

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
      int tuInRecord = pds.getTimeUnit();
      int[] intv = ptime.getInterval();
      TimeCoord.Tinv  tinv = new TimeCoord.Tinv(intv[0], intv[1]);

      if (tuInRecord != code) {
        CalendarPeriod unitInRecord = GribUtils.getCalendarPeriod(tuInRecord);
        tinv = tinv.convertReferenceDate(gr.getReferenceDate(), unitInRecord, refDate, timeUnit);
      }

      return tinv;
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<TimeCoord.Tinv> offsetSorted = new ArrayList<>(values.size());
      for (Object val : values) offsetSorted.add( (TimeCoord.Tinv) val);
      Collections.sort(offsetSorted);

      return new CoordinateTimeIntv(code, timeUnit, refDate, offsetSorted, null);
    }
  }

}
