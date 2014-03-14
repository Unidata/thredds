/*
 *
 *  * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package ucar.coord;

import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.Indent;

import java.util.*;

/**
 * Both runtime and time coordinates are tracked here
 *
 * @author caron
 * @since 1/22/14
 */
public class CoordinateTime2D extends CoordinateTimeAbstract implements Coordinate {
  private final CoordinateRuntime runtime;
  private final List<Coordinate> times; // time coordinates - original offsets
  private final CoordinateTimeAbstract otime; // time coordinates - orthogonal
  private int[] offset;  // the offset of each CoordinateTime from the base/first runtime

  private final boolean isOrthogonal;
  private final boolean isTimeInterval;
  private final int nruns;
  private final int ntimes;

  private final List<Time2D> vals; // only needed when building the GC, otherwise null

  /**
   * Ctor
   * @param code          pdsFirst.getTimeUnit()
   * @param timeUnit      time duration, based on code
   * @param vals          complete set of time coordinates, or null when reading from ncx2
   * @param runtime       list of runtimes
   * @param times         list of times, one for each runtime, offsets reletive to its runtime
   */
  public CoordinateTime2D(int code, CalendarPeriod timeUnit, List<Time2D> vals, CoordinateRuntime runtime, List<Coordinate> times) {
    super(code, timeUnit, runtime.getFirstDate());

    this.runtime = runtime;
    this.times = times;
    this.otime = null;
    this.isOrthogonal = false;
    this.isTimeInterval = times.get(0) instanceof CoordinateTimeIntv;

    int nmax = 0;
    for (Coordinate time : times)
      nmax = Math.max(nmax, time.getSize());
    ntimes = nmax;
    nruns = runtime.getSize();
    assert nruns == times.size();

    makeOffsets(times);
    this.vals = vals;
  }

  public CoordinateTime2D(int code, CalendarPeriod timeUnit, CoordinateRuntime runtime, CoordinateTimeAbstract otime) {
    super(code, timeUnit, runtime.getFirstDate());

    this.runtime = runtime;
    this.times = null;
    this.otime = otime;
    this.isOrthogonal = true;
    this.isTimeInterval = otime instanceof CoordinateTimeIntv;

    ntimes = otime.getSize();
    nruns = runtime.getSize();
    makeOffsets(otime);
    this.vals = null;
  }

  private void makeOffsets(List<Coordinate> orgTimes) {
    CalendarDate firstDate = runtime.getFirstDate();
    offset = new int[nruns];
    for (int idx=0; idx<nruns; idx++) {
      CoordinateTimeAbstract coordTime = (CoordinateTimeAbstract) orgTimes.get(idx);
      CalendarPeriod period = coordTime.getPeriod(); // LOOK are we assuming all have same period ??
      offset[idx] = period.getOffset(firstDate, runtime.getDate(idx)); // LOOK possible loss of precision
    }
  }

  private void makeOffsets(CoordinateTimeAbstract otime) {
    CalendarDate firstDate = runtime.getFirstDate();
    CalendarPeriod period = otime.getPeriod();
    offset = new int[nruns];
    for (int idx=0; idx<nruns; idx++) {
      offset[idx] = period.getOffset(firstDate, runtime.getDate(idx)); // LOOK possible loss of precision
    }
  }

  public CoordinateRuntime getRuntimeCoordinate() {
    return runtime;
  }

  /* public List<Coordinate> getTimes() {
    return times;
  } */

  public boolean isTimeInterval() {
    return isTimeInterval;
  }

  public boolean isOrthogonal() {
    return isOrthogonal;
  }

  public int getNtimes() {
    return ntimes;
  }

  public int getNruns() {
    return nruns;
  }

  public int getOffset(int runIndex) {
    return offset[runIndex];
  }

  @Override
  public void showInfo(Formatter info, Indent indent) {
    info.format("%s nruns=%d ntimes=%d isOrthogonal=%s%n", name, nruns, ntimes, isOrthogonal);
    runtime.showInfo(info, indent);
    indent.incr();
    if (isOrthogonal)
      otime.showInfo(info, indent);
    else
      for (Coordinate time : times) time.showInfo(info, indent);
    indent.decr();
  }

  @Override
  public void showCoords(Formatter info) {
    runtime.showCoords(info);
    if (isOrthogonal)
      otime.showCoords(info);
    else
      for (Coordinate time : times) time.showCoords(info);
  }

  @Override
  public List<? extends Object> getValues() {
    return vals;
  }

  @Override
  public Object getValue(int idx) {
    return (vals == null) ? null : vals.get(idx);
  }

  @Override
  public int getIndex(Object val) {
    return (vals == null) ? -1 : Collections.binarySearch(vals, (Time2D) val);
  }

  @Override
  public int getSize() {
    return (vals == null) ? 0 : vals.size();
  }

  @Override
  public Type getType() {
    return Type.time2D;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CoordinateTime2D that = (CoordinateTime2D) o;

    if (isOrthogonal != that.isOrthogonal) return false;
    if (otime != null ? !otime.equals(that.otime) : that.otime != null) return false;
    if (!runtime.equals(that.runtime)) return false;
    if (times != null ? !times.equals(that.times) : that.times != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = runtime.hashCode();
    result = 31 * result + (times != null ? times.hashCode() : 0);
    result = 31 * result + (otime != null ? otime.hashCode() : 0);
    result = 31 * result + (isOrthogonal ? 1 : 0);
    return result;
  }

  /**
   * Check if  all time intervals have the same length.
   * Only if isTimeInterval.
   * @return time interval name or MIXED_INTERVALS
   */
  public String getTimeIntervalName() {
    if (!isTimeInterval) return null;

    if (isOrthogonal)
      return ((CoordinateTimeIntv) otime).getTimeIntervalName();

    // are they the same length ?
    String firstValue = null;
    for (Coordinate timeCoord : times) {
      CoordinateTimeIntv timeCoordi = (CoordinateTimeIntv) timeCoord;
      String value = timeCoordi.getTimeIntervalName();
      if (firstValue == null) firstValue = value;
      else if (!value.equals(firstValue)) return MIXED_INTERVALS;
      else if (value.equals(MIXED_INTERVALS)) return MIXED_INTERVALS;
    }

    return firstValue;
  }

  ////////////////////////////////////////////////

  /* translate a time value in the Best coordinate to the correct offset, eg in the partition coordinate
  public Object getPartValue(int runIdx, Object bestVal) {
    Coordinate time = times.get(runIdx);
    if (isTimeInterval) {
      TimeCoord.Tinv valIntv = (TimeCoord.Tinv) bestVal;
      return valIntv.offset(-getOffset(runIdx));
    } else {
      Integer val = (Integer) bestVal;
      return val - getOffset(runIdx);
    }
  } */

  public CoordinateTimeAbstract getTimeCoordinate(int runIdx) {
    CoordinateTimeAbstract result = isOrthogonal ? otime : (CoordinateTimeAbstract) times.get(runIdx);
    if (result == null)
      System.out.println("HEY");
    return result;
  }

  public CalendarDate getRefDate(int runIdx) {
    return runtime.getDate(runIdx);
  }

  /**
   * Get the time coordinate at the given indices, into the 2D time coordinate array
   * @param runIdx     run index
   * @param timeIdx    time index
   * @return time coordinate
   */
  public Time2D getOrgValue(int runIdx, int timeIdx) {
    CoordinateTimeAbstract time = getTimeCoordinate(runIdx);
    CalendarDate runDate = runtime.getDate(runIdx);
    if (isTimeInterval) {
      TimeCoord.Tinv valIntv = (TimeCoord.Tinv) time.getValue(timeIdx);
      if (valIntv == null) return null;
      return new Time2D(runDate, null, valIntv);
    } else {
      Integer val = (Integer) time.getValue(timeIdx);
      if (val == null) return null;
      return new Time2D(runDate, val, null);
    }
  }

  /**
   * find the run and time indexes of want
   * the inverse of getOrgValue
   * @param want        find time coordinate that matches
   * @param wholeIndex  return index here
   */
  public void getIndex(Time2D want, int[] wholeIndex) {
    int runIdx = runtime.getIndex(want.run);
    CoordinateTimeAbstract time = getTimeCoordinate(runIdx);
    wholeIndex[0] = runIdx;
    if (isTimeInterval) {
      wholeIndex[1] = time.getIndex(want.tinv);
    } else {
      wholeIndex[1] = time.getIndex(want.time);
    }
  }

  /**
   * Find the index matching a runtime and time coordinate
   * @param runIdx  which run
   * @param value time coordinate
   * @param refDateOfValue reference time of time coordinate
   * @return index in the time coordinate of the value
   */
  public int matchTimeCoordinate(int runIdx, Object value, CalendarDate refDateOfValue) {
    CoordinateTimeAbstract time = getTimeCoordinate(runIdx);
    int offset = timeUnit.getOffset(time.getRefDate(), refDateOfValue);

    Object valueWithOffset;
    if (isTimeInterval) {
      TimeCoord.Tinv tinv = (TimeCoord.Tinv) value;
      valueWithOffset = tinv.offset(offset);
    } else {
      Integer val = (Integer) value;
      valueWithOffset = val + offset;
    }
    int result = time.getIndex(valueWithOffset);
    return result;
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  public CoordinateTimeAbstract makeBestTimeCoordinate(List<Double> runOffsets) {
    if (isTimeInterval) {
      Set<TimeCoord.Tinv> values = new HashSet<>();
      for (int runIdx=0; runIdx<nruns; runIdx++) {
        CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) getTimeCoordinate(runIdx);
        for (TimeCoord.Tinv tinv : timeIntv.getTimeIntervals())
          values.add(tinv.offset(getOffset(runIdx)));
      }

      List<TimeCoord.Tinv> offsetSorted = new ArrayList<>(values.size());
      for (Object val : values) offsetSorted.add( (TimeCoord.Tinv) val);
      Collections.sort(offsetSorted);
      return new CoordinateTimeIntv(getCode(), getTimeUnit(), getRefDate(), offsetSorted);

    } else {
      Set<Integer> values = new HashSet<>();
      for (int runIdx=0; runIdx<nruns; runIdx++) {
        CoordinateTime timeInt = (CoordinateTime) getTimeCoordinate(runIdx);
        for (Integer offset : timeInt.getOffsetSorted())
          values.add(offset+getOffset(runIdx));
      }

      List<Integer> offsetSorted = new ArrayList<>(values.size());
      for (Object val : values) offsetSorted.add( (Integer) val);
      Collections.sort(offsetSorted);
      return new CoordinateTime(getCode(), getTimeUnit(), getRefDate(), offsetSorted);
    }
  }

 ///////////////////////////////////////////////////////////////////////////////////////

  /**
   * for each time Coordinate in CoordBest, which runtime coordinate does it use? 1-based so 0 = missing
   *
   * @param coordBest the set of coordinates
   * @param timeInventory  whether records exist for each run, time
   * @return  array of runIdx+1, the size of cordBest
   */
  public int[] makeTime2RuntimeMap(CoordinateTimeAbstract coordBest, TwoDTimeInventory timeInventory) {
    if (isTimeInterval)
      return makeTime2RuntimeMap((CoordinateTimeIntv) coordBest, timeInventory);
    else
      return makeTime2RuntimeMap((CoordinateTime) coordBest, timeInventory);
  }

  private int[] makeTime2RuntimeMap(CoordinateTimeIntv coordBest, TwoDTimeInventory twot) {
    int[] result = new int[ coordBest.getSize()];

    Map<TimeCoord.Tinv, Integer> map = new HashMap<>();  // lookup coord val to index
    int count = 0;
    for (TimeCoord.Tinv val : coordBest.getTimeIntervals())
      map.put(val, count++);

    for (int runIdx=0; runIdx<nruns; runIdx++) {
      CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) getTimeCoordinate(runIdx);
      int timeIdx = 0;
      for (TimeCoord.Tinv bestVal : timeIntv.getTimeIntervals()) {
        // if twot == null, then we are doing a PofP
        if (twot == null || twot.getCount(runIdx, timeIdx) > 0) { // skip missing;
          Integer bestValIdx = map.get(bestVal.offset(getOffset(runIdx)));
          if (bestValIdx == null) throw new IllegalStateException();
          result[bestValIdx] = runIdx+1; // use this partition; later ones override; one based so 0 = missing
        }
        timeIdx++;
      }
    }
    return result;
  }

  private int[] makeTime2RuntimeMap(CoordinateTime coordBest, TwoDTimeInventory twot) {
    int[] result = new int[ coordBest.getSize()];

    Map<Integer, Integer> map = new HashMap<>();  // lookup coord val to index
    int count = 0;
    for (Integer val : coordBest.getOffsetSorted())
      map.put(val, count++);

    for (int runIdx=0; runIdx<nruns; runIdx++) {
      CoordinateTime timeCoord = (CoordinateTime) getTimeCoordinate(runIdx);
      int timeIdx = 0;
      for (Integer bestVal : timeCoord.getOffsetSorted()) {
        if (twot == null || twot.getCount(runIdx, timeIdx) > 0) { // skip missing;
          Integer bestValIdx = map.get(bestVal + getOffset(runIdx));
          if (bestValIdx == null) throw new IllegalStateException();
          result[bestValIdx] = runIdx+1; // use this partition; later ones override; one based so 0 = missing
        }
        timeIdx++;
      }
    }
    return result;
  }

  ////////////////////////////////////////////////////////

  /**
   * Get a sorted list of the unique time coordinates
   * @return List<Integer> or List<TimeCoord.Tinv>
   */
  public List<? extends Object> getOffsetsSorted() {
    if (isOrthogonal)
      return otime.getValues();

    if (isTimeInterval)
      return getIntervalsSorted();
    else
      return getIntegersSorted();
  }

  private List<Integer> getIntegersSorted() {
    Set<Integer> set = new HashSet<>(100);
    for (Coordinate coord : times) {
      for (Object val : coord.getValues())
        set.add((Integer) val);
    }
    List<Integer> result = new ArrayList<>();
    for (Integer val : set) result.add(val);
    Collections.sort(result);
    return result;
  }

  private List<TimeCoord.Tinv> getIntervalsSorted() {
    Set<TimeCoord.Tinv> set = new HashSet<>(100);
    for (Coordinate coord : times) {
      for (Object val : coord.getValues())
        set.add((TimeCoord.Tinv) val);
    }
    List<TimeCoord.Tinv> result = new ArrayList<>();
    for (TimeCoord.Tinv val : set) result.add(val);
    Collections.sort(result);
    return result;
  }

  ///////////////////////////////////////////////////////

  public static class Time2D implements Comparable<Time2D> {
    CalendarDate run;
    Integer time;
    TimeCoord.Tinv tinv;

    public Time2D(CalendarDate run, Integer time, TimeCoord.Tinv tinv) {
      this.run = run;
      this.time = time;
      this.tinv = tinv;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Time2D time2D = (Time2D) o;

      if (!run.equals(time2D.run)) return false;
      if (time != null ? !time.equals(time2D.time) : time2D.time != null) return false;
      if (tinv != null ? !tinv.equals(time2D.tinv) : time2D.tinv != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = run.hashCode();
      result = 31 * result + (time != null ? time.hashCode() : 0);
      result = 31 * result + (tinv != null ? tinv.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      if (time != null) return time.toString();
      else return tinv.toString();
    }

    @Override
    public int compareTo(Time2D o) {
      int r = run.compareTo(o.run);
      if (r == 0) {
        if (time != null) r = time.compareTo(o.time);
        else r = tinv.compareTo(o.tinv);
      }
      return r;
    }
  }

  /////////////////////////////////////////////////////////////////////

  public static class Builder2 extends CoordinateBuilderImpl<Grib2Record> {
    private final boolean isTimeInterval;
    private final Grib2Customizer cust;
    private final int code;                  // pdsFirst.getTimeUnit()
    private final CalendarPeriod timeUnit;   // time duration, based on code

    private final CoordinateRuntime.Builder2 runBuilder;
    private final Map<Object, CoordinateBuilderImpl<Grib2Record>> timeBuilders;

    public Builder2(boolean isTimeInterval, Grib2Customizer cust, CalendarPeriod timeUnit, int code) {
      this.isTimeInterval = isTimeInterval;
      this.cust = cust;
      this.timeUnit = timeUnit;
      this.code = code;

      runBuilder = new CoordinateRuntime.Builder2();
      timeBuilders = new HashMap<>();
    }

    public void addRecord(Grib2Record gr) {
      super.addRecord(gr);
      runBuilder.addRecord(gr);
      Time2D val = (Time2D) extract(gr);
      CoordinateBuilderImpl<Grib2Record> timeBuilder = timeBuilders.get(val.run);
      timeBuilder.addRecord(gr);
    }

    @Override
    public Object extract(Grib2Record gr) {
      CalendarDate run = (CalendarDate) runBuilder.extract(gr);
      CoordinateBuilderImpl<Grib2Record> timeBuilder = timeBuilders.get(run);
      if (timeBuilder == null) {
        timeBuilder = isTimeInterval ? new CoordinateTimeIntv.Builder2(cust, code, timeUnit, run) : new CoordinateTime.Builder2(code, timeUnit, run);
        timeBuilders.put(run, timeBuilder);
      }
      Object time = timeBuilder.extract(gr);
      if (time instanceof Integer)
        return new Time2D(run, (Integer) time, null);
      else
        return new Time2D(run, null, (TimeCoord.Tinv) time);
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      CoordinateRuntime runCoord = (CoordinateRuntime) runBuilder.finish();

      List<Coordinate> times = new ArrayList<>();
      for (CalendarDate runtimeDate : runCoord.getRuntimesSorted()) {
        CoordinateBuilderImpl<Grib2Record> timeBuilder = timeBuilders.get(runtimeDate);
        times.add(timeBuilder.finish());
      }

      List<Time2D> vals = new ArrayList<>(values.size());
      for (Object val : values) vals.add( (Time2D) val);
      Collections.sort(vals);

      return new CoordinateTime2D(code, timeUnit, vals, runCoord, times);
    }

    @Override
    public void addAll(Coordinate coord) {
     super.addAll(coord);
     for (Object val : coord.getValues()) {
       Time2D val2D = (Time2D) val;
       runBuilder.add( val2D.run);
       CoordinateBuilderImpl<Grib2Record> timeBuilder = timeBuilders.get(val2D.run);
       if (timeBuilder == null) {
         timeBuilder = isTimeInterval ? new CoordinateTimeIntv.Builder2(cust, code, timeUnit, val2D.run) : new CoordinateTime.Builder2(code, timeUnit, val2D.run);
         timeBuilders.put(val2D.run, timeBuilder);
       }
       timeBuilder.add(isTimeInterval ? val2D.tinv : val2D.time);
     }
    }

  }

  public static class Builder1 extends CoordinateBuilderImpl<Grib1Record> {
    private final boolean isTimeInterval;
    private final Grib1Customizer cust;
    private final int code;                  // pdsFirst.getTimeUnit()
    private final CalendarPeriod timeUnit;

    private final CoordinateRuntime.Builder1 runBuilder;
    private final Map<Object, CoordinateBuilderImpl<Grib1Record>> timeBuilders;

    public Builder1(boolean isTimeInterval, Grib1Customizer cust, CalendarPeriod timeUnit, int code) {
      this.isTimeInterval = isTimeInterval;
      this.cust = cust;
      this.timeUnit = timeUnit;
      this.code = code;

      runBuilder = new CoordinateRuntime.Builder1();
      timeBuilders = new HashMap<>();
    }

    public void addRecord(Grib1Record gr) {
      super.addRecord(gr);
      runBuilder.addRecord(gr);
      Time2D val = (Time2D) extract(gr);
      CoordinateBuilderImpl<Grib1Record> timeBuilder = timeBuilders.get(val.run);
      timeBuilder.addRecord(gr);
    }

    @Override
    public Object extract(Grib1Record gr) {
      CalendarDate run = (CalendarDate) runBuilder.extract(gr);
      CoordinateBuilderImpl<Grib1Record> timeBuilder = timeBuilders.get(run);
      if (timeBuilder == null) {
        timeBuilder = isTimeInterval ? new CoordinateTimeIntv.Builder1(cust, code, timeUnit, run) : new CoordinateTime.Builder1(code, timeUnit, run);
        timeBuilders.put(run, timeBuilder);
      }
      Object time = timeBuilder.extract(gr);
      if (time instanceof Integer)
        return new Time2D(run, (Integer) time, null);
      else
        return new Time2D(run, null, (TimeCoord.Tinv) time);
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      CoordinateRuntime runCoord = (CoordinateRuntime) runBuilder.finish();

      List<Coordinate> times = new ArrayList<>();
      for (CalendarDate runtimeDate : runCoord.getRuntimesSorted()) {
        CoordinateBuilderImpl<Grib1Record> timeBuilder = timeBuilders.get(runtimeDate);
        times.add(timeBuilder.finish());
      }

      List<Time2D> vals = new ArrayList<>(values.size());
      for (Object val : values) vals.add( (Time2D) val);
      Collections.sort(vals);

      return new CoordinateTime2D(code, timeUnit, vals, runCoord, times);
    }

    @Override
    public void addAll(Coordinate coord) {
     super.addAll(coord);
     for (Object val : coord.getValues()) {
       Time2D val2D = (Time2D) val;
       runBuilder.add( val2D.run);
       CoordinateBuilderImpl<Grib1Record> timeBuilder = timeBuilders.get(val2D.run);
       if (timeBuilder == null) {
         timeBuilder = isTimeInterval ? new CoordinateTimeIntv.Builder1(cust, code, timeUnit, val2D.run) : new CoordinateTime.Builder1(code, timeUnit, val2D.run);
         timeBuilders.put(val2D.run, timeBuilder);
       }
       timeBuilder.add(isTimeInterval ? val2D.tinv : val2D.time);
     }
    }

  }

}
