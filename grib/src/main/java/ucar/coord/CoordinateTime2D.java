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

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.collection.GribIosp;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.Indent;

import java.util.*;

/**
 * Both runtime and time coordinates are tracked here. The time coordinate is dependent on the runtime, at least on the offset.
 *
 * @author caron
 * @since 1/22/14
 */
@Immutable
public class CoordinateTime2D extends CoordinateTimeAbstract implements Coordinate {
  private final CoordinateRuntime runtime;
  private final List<Coordinate> times;       // nruns time coordinates - original offsets
  private final CoordinateTimeAbstract otime; // orthogonal time coordinates - only when isOrthogonal
  private final SortedMap<Integer,CoordinateTimeAbstract> regTimes; // only when isRegular: <hour of day, time coordinate>
  private final int[] offset;          // the offset of each CoordinateTime from the base/first runtime, length nruns  (LOOK can we use SmartArrayInt ?)

  private final boolean isRegular;
  private final boolean isOrthogonal;
  private final boolean isTimeInterval;
  private final int nruns;
  private final int ntimes;

  private final List<Time2D> vals;  // only present when building the GC, otherwise null

  /**
   * Ctor. Most general, a CoordinateTime for each runtime.
   *
   * @param code          pdsFirst.getTimeUnit()
   * @param timeUnit      time duration, based on code
   * @param vals          complete set of Time2D values, may be null (used only during creation)
   * @param runtime       list of runtimes
   * @param times         list of times, one for each runtime, offsets reletive to its runtime, may not be null
   */
  public CoordinateTime2D(int code, CalendarPeriod timeUnit, List<Time2D> vals, CoordinateRuntime runtime, List<Coordinate> times) {
    super(code, timeUnit, runtime.getFirstDate(), null);

    this.runtime = runtime;
    this.times = Collections.unmodifiableList(times);
    this.otime = null;
    this.regTimes = null;
    this.isRegular = false;
    this.isOrthogonal = false;
    this.isTimeInterval = times.get(0) instanceof CoordinateTimeIntv;

    int nmax = 0;
    for (Coordinate time : times)
      nmax = Math.max(nmax, time.getSize());
    this.ntimes = nmax;
    this.nruns = runtime.getSize();
    assert nruns == times.size();

    this.offset = makeOffsets(times);
    this.vals = (vals == null) ? null : Collections.unmodifiableList(vals);
  }

  /**
   * Ctor. orthogonal - all offsets are the same for all runtimes, so 2d time array is (runtime X otime)
   *
   * @param code          pdsFirst.getTimeUnit()
   * @param timeUnit      time duration, based on code
   * @param vals          complete set of Time2D values, may be null (used only during creation)
   * @param runtime       list of runtimes
   * @param otime         list of offsets, all the same for each runtime
   * @param times         list of times, one for each runtime, offsets reletive to its runtime, may be null (Only available during creation, not stored in index)
   */
  public CoordinateTime2D(int code, CalendarPeriod timeUnit, List<Time2D> vals, CoordinateRuntime runtime, CoordinateTimeAbstract otime, List<Coordinate> times) {
    super(code, timeUnit, runtime.getFirstDate(), null);

    this.runtime = runtime;
    this.times = (times == null) ? null : Collections.unmodifiableList(times);    // need these for makeBest

    this.otime = otime;
    this.isOrthogonal = true;
    this.isRegular = false;
    this.regTimes = null;
    this.isTimeInterval = otime instanceof CoordinateTimeIntv;
    this.ntimes = otime.getSize();

    this.nruns = runtime.getSize();
    this.offset = makeOffsets(timeUnit);
    this.vals = (vals == null) ? null : Collections.unmodifiableList(vals);
  }

  /**
   * Ctor. regular - all offsets are the same for each "runtime hour of day", eg all 0Z runtimes have the same offsets, all 6Z runtimes have the same offsets, etc.
   * 2d time array is (runtime X otime(hour), where hour = runtime hour of day
   *
   * @param code          pdsFirst.getTimeUnit()
   * @param timeUnit      time duration, based on code
   * @param vals          complete set of Time2D values, may be null (used only during creation)
   * @param runtime       list of runtimes
   * @param regList       list of offsets, one each for each possible runtime hour of day.
   * @param times         list of times, one for each runtime, offsets reletive to its runtime, may be null (Only available during creation, not stored in index)
   */
  public CoordinateTime2D(int code, CalendarPeriod timeUnit, List<Time2D> vals, CoordinateRuntime runtime, List<Coordinate> regList, List<Coordinate> times) {
    super(code, timeUnit, runtime.getFirstDate(), null);

    this.runtime = runtime;
    this.nruns = runtime.getSize();

    this.times = (times == null) ? null : Collections.unmodifiableList(times);    // need these for makeBest
    this.otime = null;
    this.isOrthogonal = false;
    this.isRegular = true;
    CoordinateTimeAbstract first = (CoordinateTimeAbstract) regList.get(0);
    this.isTimeInterval = first instanceof CoordinateTimeIntv;

    // regList may have different lengths
    int nmax = 0;
    for (Coordinate time : regList)
      nmax = Math.max(nmax, time.getSize());
    this.ntimes = nmax;

    // make the offset map
    this.regTimes = new TreeMap<>();
    for (Coordinate coord : regList) {
      CoordinateTimeAbstract time = (CoordinateTimeAbstract) coord;
      CalendarDate ref = time.getRefDate();
      int hour = ref.getHourOfDay();
      this.regTimes.put(hour, time);
    }

    this.offset = makeOffsets(timeUnit);
    this.vals = (vals == null) ? null : Collections.unmodifiableList(vals);
  }

  private int[] makeOffsets(List<Coordinate> orgTimes) {
    CalendarDate firstDate = runtime.getFirstDate();
    int[] offsets = new int[nruns];
    for (int idx=0; idx<nruns; idx++) {
      CoordinateTimeAbstract coordTime = (CoordinateTimeAbstract) orgTimes.get(idx);
      CalendarPeriod period = coordTime.getPeriod(); // LOOK are we assuming all have same period ??
      offsets[idx] = period.getOffset(firstDate, runtime.getRuntimeDate(idx)); // LOOK possible loss of precision
    }
    return offsets;
  }

  private int[] makeOffsets(CalendarPeriod period) {
    CalendarDate firstDate = runtime.getFirstDate();
    int[] offsets = new int[nruns];
    for (int idx=0; idx<nruns; idx++) {
      offsets[idx] = period.getOffset(firstDate, runtime.getRuntimeDate(idx)); // LOOK possible loss of precision
    }
    return offsets;
  }

  public CoordinateRuntime getRuntimeCoordinate() {
    return runtime;
  }

  public boolean isTimeInterval() {
    return isTimeInterval;
  }

  public boolean isOrthogonal() {
    return isOrthogonal;
  }

  public boolean isRegular() {
    return isRegular;
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
    info.format("%s nruns=%d ntimes=%d isOrthogonal=%s isRegular=%s%n", name, nruns, ntimes, isOrthogonal, isRegular);
    runtime.showInfo(info, indent);
    indent.incr();

    info.format("%nAll time values=");
    List timeValues = getOffsetsSorted();
    for (Object val : timeValues) info.format(" %s,", val);
    info.format(" (n=%d)%n%n", timeValues.size());

    if (isOrthogonal)
      otime.showInfo(info, indent);

    else if (isRegular)
      for (int hour : regTimes.keySet()) {
        CoordinateTimeAbstract timeCoord = regTimes.get(hour);
        info.format( "%shour %d: ", indent, hour);
        timeCoord.showInfo(info, new Indent(0));
      }

    else
      for (Coordinate time : times) {
        info.format( "%s%s:", indent, ((CoordinateTimeAbstract)time).getRefDate());
        time.showInfo(info, new Indent(0));
      }
    indent.decr();
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("%s nruns=%d ntimes=%d isOrthogonal=%s isRegular=%s%n", name, nruns, ntimes, isOrthogonal, isRegular);
    runtime.showCoords(info);

    if (isOrthogonal)
      otime.showCoords(info);

    else if (isRegular)
      for (int hour : regTimes.keySet()) {
        CoordinateTimeAbstract timeCoord = regTimes.get(hour);
        info.format("hour %d: ", hour);
        timeCoord.showInfo(info, new Indent(0));
      }

    else
      for (Coordinate time : times) {
        time.showCoords(info);
      }
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
  public int estMemorySize() {
    return 864 + nruns * (48+4)  + ntimes * 24;  // nruns * (calendar date + integer)  + ntimes + integer)
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
    if (isTimeInterval != that.isTimeInterval) return false;
    if (!runtime.equals(that.runtime)) return false;
    if (isOrthogonal != that.isOrthogonal) return false;
    if (isRegular != that.isRegular) return false;
    if (otime != null ? !otime.equals(that.otime) : that.otime != null) return false;
    if (regTimes != null ? !regTimes.equals(that.regTimes) : that.regTimes != null) return false;
    if (times != null ? !times.equals(that.times) : that.times != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = runtime.hashCode();
    result = 31 * result + (times != null ? times.hashCode() : 0);
    result = 31 * result + (otime != null ? otime.hashCode() : 0);
    result = 31 * result + (regTimes != null ? regTimes.hashCode() : 0);
    result = 31 * result + (isRegular ? 1 : 0);
    result = 31 * result + (isOrthogonal ? 1 : 0);
    result = 31 * result + (isTimeInterval ? 1 : 0);
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

    if (isRegular) {
      String firstValue = null;
      for (CoordinateTimeAbstract timeCoord : regTimes.values()) {
        CoordinateTimeIntv timeCoordi = (CoordinateTimeIntv) timeCoord;
        String value = timeCoordi.getTimeIntervalName();
        if (firstValue == null) firstValue = value;
        else if (!value.equals(firstValue)) return MIXED_INTERVALS;
        else if (value.equals(MIXED_INTERVALS)) return MIXED_INTERVALS;
      }
      return firstValue;
    }

    // are they the same length ?
    String firstValue = null;
    for (Coordinate timeCoord : times) {
      if (times.size() == 0) continue; // skip empties
      CoordinateTimeIntv timeCoordi = (CoordinateTimeIntv) timeCoord;
      String value = timeCoordi.getTimeIntervalName();
      if (firstValue == null) firstValue = value;
      else if (!value.equals(firstValue)) return MIXED_INTERVALS;
      else if (value.equals(MIXED_INTERVALS)) return MIXED_INTERVALS;
    }
    return firstValue;
  }

  @Override
  public CalendarDateRange makeCalendarDateRange(ucar.nc2.time.Calendar cal) {

    CoordinateTimeAbstract firstCoord = getTimeCoordinate(0);
    CoordinateTimeAbstract lastCoord = getTimeCoordinate(nruns-1);

    CalendarDateRange firstRange = firstCoord.makeCalendarDateRange(cal);
    CalendarDateRange lastRange = lastCoord.makeCalendarDateRange(cal);

    return CalendarDateRange.of(firstRange.getStart(), lastRange.getEnd());
  }

  ////////////////////////////////////////////////

  public CoordinateTimeAbstract getTimeCoordinate(int runIdx) {
    if (isOrthogonal)
      return factory(otime, getRefDate(runIdx));  // LOOK problem is cant use time.getRefDate(), must use time2D.getRefDate(runIdx) !!

    if (isRegular) {
      CalendarDate ref = getRefDate(runIdx);
      int hour = ref.getHourOfDay();
      return regTimes.get(hour);
    }

    return (CoordinateTimeAbstract) times.get(runIdx);
  }

  public CalendarDate getRefDate(int runIdx) {
    return runtime.getRuntimeDate(runIdx);
  }

  public long getRuntime(int runIdx) {
    return runtime.getRuntime(runIdx);
  }

  private CoordinateTimeAbstract factory(CoordinateTimeAbstract org, CalendarDate refDate) {
    if (isTimeInterval) {
      return new CoordinateTimeIntv((CoordinateTimeIntv) org, refDate);
    } else {
      return new CoordinateTime((CoordinateTime)org, refDate);
    }
  }

  /**
   * Get the time coordinate at the given indices, into the 2D time coordinate array
   * @param runIdx     run index
   * @param timeIdx    time index
   * @return time coordinate
   */
  public Time2D getOrgValue(int runIdx, int timeIdx, boolean debug) {
    CoordinateTimeAbstract time = getTimeCoordinate(runIdx);
    CalendarDate runDate = runtime.getRuntimeDate(runIdx);
    if (isTimeInterval) {
      TimeCoord.Tinv valIntv = (TimeCoord.Tinv) time.getValue(timeIdx);
      //if (debug) System.out.printf("    coordTime2D intv runDate=%s time.getValue(timeIdx)=%s%n", runDate, valIntv);
      if (valIntv == null) return null;
      return new Time2D(runDate, null, valIntv);
    } else {
      Integer val = (Integer) time.getValue(timeIdx);
      //if (debug) System.out.printf("    coordTime2D int runDate=%s time.getValue(timeIdx)=%s%n", runDate, val);
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
  public boolean getIndex(Time2D want, int[] wholeIndex) {
    int runIdx = runtime.getIndex(want.run);
    CoordinateTimeAbstract time = getTimeCoordinate(runIdx);
    wholeIndex[0] = runIdx;
    if (isTimeInterval) {
      wholeIndex[1] = time.getIndex(want.tinv);
    } else {
      wholeIndex[1] = time.getIndex(want.time);
    }
    return (wholeIndex[0] >= 0)  && (wholeIndex[1] >= 0);
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
    int offset = timeUnit.getOffset(getRefDate(runIdx), refDateOfValue);

    Object valueWithOffset;
    if (isTimeInterval) {
      TimeCoord.Tinv tinv = (TimeCoord.Tinv) value;
      valueWithOffset = tinv.offset(offset);
    } else {
      Integer val = (Integer) value;
      valueWithOffset = val + offset;
    }
    int result = time.getIndex(valueWithOffset);
    if (GribIosp.debugRead) System.out.printf("  matchTimeCoordinate value wanted = (%s) valueWithOffset=%s result=%d %n", value, valueWithOffset, result);

    return result;
  }

  ///////////////////////////////////////////////////////////////////////////////////////

  protected CoordinateTimeAbstract makeBestFromComplete(int[] best, int n) {
    throw new UnsupportedOperationException();
  }

  public CoordinateTimeAbstract makeBestTimeCoordinate(CoordinateRuntime master) {
    if (isTimeInterval) {
      return makeBestTimeIntv(master);
    } else {
      return makeBestTime(master);
    }
  }

  private CoordinateTimeAbstract makeBestTime(CoordinateRuntime master) {
    // make complete, unique set of coordinates
    Set<Integer> values = new HashSet<>();  // complete set of values
    for (int runIdx=0; runIdx<nruns; runIdx++) {   // use times array, passed into constructor, with original inventory, if possible
      CoordinateTime timeCoord = (times == null) ?  (CoordinateTime)  getTimeCoordinate(runIdx) : (CoordinateTime) times.get(runIdx);
      for (Integer offset : timeCoord.getOffsetSorted())
        values.add(offset+getOffset(runIdx));
    }
    List<Integer> offsetSorted = new ArrayList<>(values.size());
    for (Object val : values) offsetSorted.add( (Integer) val);
    Collections.sort(offsetSorted);   // complete set of values

    // fast lookup of offset val in the result CoordinateTime
    Map<Integer, Integer> map = new HashMap<>();
    int count = 0;
    for (Integer val : offsetSorted)
      map.put(val, count++);

    // fast lookup of the run time in the master
    int[] run2master = new int[nruns];
    int masterIdx = 0;
    for (int run2Didx=0; run2Didx<nruns; run2Didx++) {
      while (!master.getRuntimeDate(masterIdx).equals( runtime.getRuntimeDate(run2Didx)))
        masterIdx++;
      run2master[run2Didx] = masterIdx;
      masterIdx++;
    }
    assert masterIdx >= nruns;

    // now for each coordinate, use the latest runtime available
    int[] time2runtime = new int[ offsetSorted.size()];
    for (int runIdx=0; runIdx<nruns; runIdx++) {
      CoordinateTime timeCoord = (times == null) ? (CoordinateTime) getTimeCoordinate(runIdx) : (CoordinateTime) times.get(runIdx);
      for (Integer offset : timeCoord.getOffsetSorted()) {
        Integer bestValIdx = map.get(offset + getOffset(runIdx));
        if (bestValIdx == null) throw new IllegalStateException();
        time2runtime[bestValIdx] = run2master[runIdx] + 1; // uses this runtime; later ones override; one based so 0 = missing
      }
    }

    return new CoordinateTime(getCode(), getTimeUnit(), getRefDate(), offsetSorted, time2runtime);
  }

  private CoordinateTimeAbstract makeBestTimeIntv(CoordinateRuntime master) {
     // make unique set of coordinates
    Set<TimeCoord.Tinv> values = new HashSet<>();
    for (int runIdx=0; runIdx<nruns; runIdx++) {        // use times array, passed into constructor, with original inventory, if possible
      CoordinateTimeIntv timeIntv = (times == null) ?  (CoordinateTimeIntv)  getTimeCoordinate(runIdx) : (CoordinateTimeIntv) times.get(runIdx);
      for (TimeCoord.Tinv tinv : timeIntv.getTimeIntervals())
        values.add(tinv.offset(getOffset(runIdx)));
    }
    List<TimeCoord.Tinv> offsetSorted = new ArrayList<>(values.size());
    for (Object val : values) offsetSorted.add( (TimeCoord.Tinv) val);
    Collections.sort(offsetSorted);

    // fast lookup of offset tinv in the result CoordinateTimeIntv
    Map<TimeCoord.Tinv, Integer> map = new HashMap<>();  // lookup coord val to index
    int count = 0;
    for (TimeCoord.Tinv val : offsetSorted)
      map.put(val, count++);

        // fast lookup of the run time in the master
    int[] run2master = new int[nruns];
    int masterIdx = 0;
    for (int run2Didx=0; run2Didx<nruns; run2Didx++) {
      while (!master.getRuntimeDate(masterIdx).equals( runtime.getRuntimeDate(run2Didx)))
        masterIdx++;
      run2master[run2Didx] = masterIdx;
      masterIdx++;
    }
    assert masterIdx >= nruns;

    int[] time2runtime = new int[ offsetSorted.size()];
    for (int runIdx=0; runIdx<nruns; runIdx++) {
      CoordinateTimeIntv timeIntv = (times == null) ?  (CoordinateTimeIntv)  getTimeCoordinate(runIdx) : (CoordinateTimeIntv) times.get(runIdx);
      for (TimeCoord.Tinv bestVal : timeIntv.getTimeIntervals()) {
        Integer bestValIdx = map.get(bestVal.offset(getOffset(runIdx)));
        if (bestValIdx == null) throw new IllegalStateException();
        time2runtime[bestValIdx] = run2master[runIdx] + 1; // uses this runtime; later ones override; one based so 0 = missing
      }
    }

    return new CoordinateTimeIntv(getCode(), getTimeUnit(), getRefDate(), offsetSorted, time2runtime);
  }

  ////////////////////////////////////////////////////////

  /**
   * public by accident - do not use
   */
  public List<? extends Coordinate> getTimesForSerialization() {
    if (isOrthogonal) {
      List<Coordinate> list = new ArrayList<>(1);
      list.add(otime);
      return list;
    } else if (isRegular) {
      return new ArrayList<>(regTimes.values());
    } else {
      return times;
    }
  }

  /**
   * Get a sorted list of the unique time coordinates
   * @return List<Integer> or List<TimeCoord.Tinv>
   */
  public List<? extends Object> getOffsetsSorted() {
    if (isOrthogonal)
      return otime.getValues();

    List<? extends Coordinate> coords = isRegular ? new ArrayList<>(regTimes.values()) : times;
    if (isTimeInterval)
      return getIntervalsSorted(coords);
    else
      return getIntegersSorted(coords);
  }

  private List<Integer> getIntegersSorted(List<? extends Coordinate> coords) {
    Set<Integer> set = new HashSet<>(100);
    for (Coordinate coord : coords) {
      for (Object val : coord.getValues())
        set.add((Integer) val);
    }
    List<Integer> result = new ArrayList<>();
    for (Integer val : set) result.add(val);
    Collections.sort(result);
    return result;
  }

  private List<TimeCoord.Tinv> getIntervalsSorted(List<? extends Coordinate> coords) {
    Set<TimeCoord.Tinv> set = new HashSet<>(100);
    for (Coordinate coord : coords) {
      for (Object val : coord.getValues())
        set.add((TimeCoord.Tinv) val);
    }
    List<TimeCoord.Tinv> result = new ArrayList<>();
    for (TimeCoord.Tinv val : set) result.add(val);
    Collections.sort(result);
    return result;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static class Time2D implements Comparable<Time2D> {
    long run;
    Integer time;
    TimeCoord.Tinv tinv;

    public Time2D(CalendarDate run, Integer time, TimeCoord.Tinv tinv) {
      this.run = run.getMillis();
      this.time = time;
      this.tinv = tinv;
    }

    public Time2D(long run, Integer time, TimeCoord.Tinv tinv) {
      this.run = run;
      this.time = time;
      this.tinv = tinv;
    }

    public CalendarDate getRefDate() {
      return CalendarDate.of(run);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Time2D time2D = (Time2D) o;

      if (run != time2D.run) return false;
      if (time != null ? !time.equals(time2D.time) : time2D.time != null) return false;
      if (tinv != null ? !tinv.equals(time2D.tinv) : time2D.tinv != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = (int) (run ^ (run >>> 32));
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
      int r = Long.compare(run, o.run);
      if (r == 0) {
        if (time != null) r = time.compareTo(o.time);
        else r = tinv.compareTo(o.tinv);
      }
      return r;
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static class Builder2 extends CoordinateBuilderImpl<Grib2Record> implements CoordinateBuilder.TwoD<Grib2Record> {
    private final boolean isTimeInterval;
    private final Grib2Customizer cust;
    private final int code;                  // pdsFirst.getTimeUnit()
    private final CalendarPeriod timeUnit;   // time duration, based on code

    private final CoordinateRuntime.Builder2 runBuilder;
    private final Map<Object, CoordinateBuilderImpl<Grib2Record>> timeBuilders;  // one for each runtime

    public Builder2(boolean isTimeInterval, Grib2Customizer cust, CalendarPeriod timeUnit, int code) {
      this.isTimeInterval = isTimeInterval;
      this.cust = cust;
      this.timeUnit = timeUnit;
      this.code = code;

      runBuilder = new CoordinateRuntime.Builder2(timeUnit);
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
      Long run = (Long) runBuilder.extract(gr);
      CoordinateBuilderImpl<Grib2Record> timeBuilder = timeBuilders.get(run);
      if (timeBuilder == null) {
        timeBuilder = isTimeInterval ? new CoordinateTimeIntv.Builder2(cust, code, timeUnit, CalendarDate.of(run)) :
                new CoordinateTime.Builder2(code, timeUnit,  CalendarDate.of(run));
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

      List<Coordinate> times = new ArrayList<>(runCoord.getSize());
      for (int idx=0; idx<runCoord.getSize(); idx++) {
        Long runtime = runCoord.getRuntime(idx);
        CoordinateBuilderImpl<Grib2Record> timeBuilder = timeBuilders.get(runtime);
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
         timeBuilder = isTimeInterval ? new CoordinateTimeIntv.Builder2(cust, code, timeUnit, val2D.getRefDate()) : new CoordinateTime.Builder2(code, timeUnit, val2D.getRefDate());
         timeBuilders.put(val2D.run, timeBuilder);
       }
       timeBuilder.add(isTimeInterval ? val2D.tinv : val2D.time);
     }
    }

    @Override
    public int[] getCoordIndices(Grib2Record gr) {
      CoordinateTime2D coord2D = (CoordinateTime2D) coord;
      Long run = (Long) runBuilder.extract(gr);
      int runIdx = coord2D.runtime.getIndex(run);
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);

      CoordinateBuilderImpl<Grib2Record> timeBuilder = timeBuilders.get(run);
      Object time = timeBuilder.extract(gr);
      int timeIdx = timeCoord.getIndex(time);

      return  new int[] {runIdx, timeIdx};
    }

  }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static class Builder1 extends CoordinateBuilderImpl<Grib1Record> implements CoordinateBuilder.TwoD<Grib1Record> {
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

      runBuilder = new CoordinateRuntime.Builder1(timeUnit);
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
      Long run = (Long) runBuilder.extract(gr);
      CoordinateBuilderImpl<Grib1Record> timeBuilder = timeBuilders.get(run);
      if (timeBuilder == null) {
        timeBuilder = isTimeInterval ? new CoordinateTimeIntv.Builder1(cust, code, timeUnit, CalendarDate.of(run)) :
                new CoordinateTime.Builder1(cust, code, timeUnit, CalendarDate.of(run));
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

      List<Coordinate> times = new ArrayList<>(runCoord.getSize());
      for (int idx=0; idx<runCoord.getSize(); idx++) {
        Long runtime = runCoord.getRuntime(idx);
        CoordinateBuilderImpl<Grib1Record> timeBuilder = timeBuilders.get(runtime);
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
         timeBuilder = isTimeInterval ? new CoordinateTimeIntv.Builder1(cust, code, timeUnit, val2D.getRefDate()) : new CoordinateTime.Builder1(cust, code, timeUnit, val2D.getRefDate());
         timeBuilders.put(val2D.run, timeBuilder);
       }
       timeBuilder.add(isTimeInterval ? val2D.tinv : val2D.time);
     }
    }

    @Override
    public int[] getCoordIndices(Grib1Record gr) {
      CoordinateTime2D coord2D = (CoordinateTime2D) coord;
      Long run = (Long) runBuilder.extract(gr);
      int runIdx = coord2D.runtime.getIndex(run);
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);

      CoordinateBuilderImpl<Grib1Record> timeBuilder = timeBuilders.get(run);
      Object time = timeBuilder.extract(gr);
      int timeIdx = timeCoord.getIndex(time);

      return  new int[] {runIdx, timeIdx};
    }
  }

}

/*
TwoD/time1 =
  {
    {0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0, 51.0, 54.0, 57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0},
    {6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0, 51.0, 54.0, 57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0},
    {12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0, 51.0, 54.0, 57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0},
    {18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0, 51.0, 54.0, 57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0, 99.0, 102.0},
    {24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0, 51.0, 54.0, 57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0, 99.0, 102.0, 105.0, 108.0},
    {30.0, 33.0, 36.0, 39.0, 42.0, 45.0, 48.0, 51.0, 54.0, 57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0, 99.0, 102.0, 105.0, 108.0, 111.0, 114.0},
    {36.0, 39.0, 42.0, 45.0, 48.0, 51.0, 54.0, 57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0, 99.0, 102.0, 105.0, 108.0, 111.0, 114.0, 117.0, 120.0},
    {42.0, 45.0, 48.0, 51.0, 54.0, 57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0, 99.0, 102.0, 105.0, 108.0, 111.0, 114.0, 117.0, 120.0, 123.0, 126.0},
    {48.0, 51.0, 54.0, 57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0, 99.0, 102.0, 105.0, 108.0, 111.0, 114.0, 117.0, 120.0, 123.0, 126.0, 129.0, 132.0},
    {54.0, 57.0, 60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0, 99.0, 102.0, 105.0, 108.0, 111.0, 114.0, 117.0, 120.0, 123.0, 126.0, 129.0, 132.0, 135.0, 138.0},
    {60.0, 63.0, 66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0, 99.0, 102.0, 105.0, 108.0, 111.0, 114.0, 117.0, 120.0, 123.0, 126.0, 129.0, 132.0, 135.0, 138.0, 141.0, 144.0},
    {66.0, 69.0, 72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0, 99.0, 102.0, 105.0, 108.0, 111.0, 114.0, 117.0, 120.0, 123.0, 126.0, 129.0, 132.0, 135.0, 138.0, 141.0, 144.0, 147.0, 150.0},
    {72.0, 75.0, 78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0, 99.0, 102.0, 105.0, 108.0, 111.0, 114.0, 117.0, 120.0, 123.0, 126.0, 129.0, 132.0, 135.0, 138.0, 141.0, 144.0, 147.0, 150.0, 153.0, 156.0},
    {78.0, 81.0, 84.0, 87.0, 90.0, 93.0, 96.0, 99.0, 102.0, 105.0, 108.0, 111.0, 114.0, 117.0, 120.0, 123.0, 126.0, 129.0, 132.0, 135.0, 138.0, 141.0, 144.0, 147.0, 150.0, 153.0, 156.0, 159.0, 162.0},
    {84.0, 87.0, 90.0, 93.0, 96.0, 99.0, 102.0, 105.0, 108.0, 111.0, 114.0, 117.0, 120.0, 123.0, 126.0, 129.0, 132.0, 135.0, 138.0, 141.0, 144.0, 147.0, 150.0, 153.0, 156.0, 159.0, 162.0, 165.0, 168.0},
    {90.0, 93.0, 96.0, 99.0, 102.0, 105.0, 108.0, 111.0, 114.0, 117.0, 120.0, 123.0, 126.0, 129.0, 132.0, 135.0, 138.0, 141.0, 144.0, 147.0, 150.0, 153.0, 156.0, 159.0, 162.0, 165.0, 168.0, 171.0, 174.0},
    {96.0, 99.0, 102.0, 105.0, 108.0, 111.0, 114.0, 117.0, 120.0, 123.0, 126.0, 129.0, 132.0, 135.0, 138.0, 141.0, 144.0, 147.0, 150.0, 153.0, 156.0, 159.0, 162.0, 165.0, 168.0, 171.0, 174.0, 177.0, 180.0},
    {102.0, 105.0, 108.0, 111.0, 114.0, 117.0, 120.0, 123.0, 126.0, 129.0, 132.0, 135.0, 138.0, 141.0, 144.0, 147.0, 150.0, 153.0, 156.0, 159.0, 162.0, 165.0, 168.0, 171.0, 174.0, 177.0, 180.0, 183.0, 186.0},
    {108.0, 111.0, 114.0, 117.0, 120.0, 123.0, 126.0, 129.0, 132.0, 135.0, 138.0, 141.0, 144.0, 147.0, 150.0, 153.0, 156.0, 159.0, 162.0, 165.0, 168.0, 171.0, 174.0, 177.0, 180.0, 183.0, 186.0, 189.0, 192.0},
    {114.0, 117.0, 120.0, 123.0, 126.0, 129.0, 132.0, 135.0, 138.0, 141.0, 144.0, 147.0, 150.0, 153.0, 156.0, 159.0, 162.0, 165.0, 168.0, 171.0, 174.0, 177.0, 180.0, 183.0, 186.0, 189.0, 192.0, 195.0, 198.0},
    {120.0, 123.0, 126.0, 129.0, 132.0, 135.0, 138.0, 141.0, 144.0, 147.0, 150.0, 153.0, 156.0, 159.0, 162.0, 165.0, 168.0, 171.0, 174.0, 177.0, 180.0, 183.0, 186.0, 189.0, 192.0, 195.0, 198.0, 201.0, 204.0},
    {126.0, 129.0, 132.0, 135.0, 138.0, 141.0, 144.0, 147.0, 150.0, 153.0, 156.0, 159.0, 162.0, 165.0, 168.0, 171.0, 174.0, 177.0, 180.0, 183.0, 186.0, 189.0, 192.0, 195.0, 198.0, 201.0, 204.0, 207.0, 210.0},
    {132.0, 135.0, 138.0, 141.0, 144.0, 147.0, 150.0, 153.0, 156.0, 159.0, 162.0, 165.0, 168.0, 171.0, 174.0, 177.0, 180.0, 183.0, 186.0, 189.0, 192.0, 195.0, 198.0, 201.0, 204.0, 207.0, 210.0, 213.0, 216.0},
    {138.0, 141.0, 144.0, 147.0, 150.0, 153.0, 156.0, 159.0, 162.0, 165.0, 168.0, 171.0, 174.0, 177.0, 180.0, 183.0, 186.0, 189.0, 192.0, 195.0, 198.0, 201.0, 204.0, 207.0, 210.0, 213.0, 216.0, 219.0, 222.0},
    {144.0, 147.0, 150.0, 153.0, 156.0, 159.0, 162.0, 165.0, 168.0, 171.0, 174.0, 177.0, 180.0, 183.0, 186.0, 189.0, 192.0, 195.0, 198.0, 201.0, 204.0, 207.0, 210.0, 213.0, 216.0, 219.0, 222.0, 225.0, 228.0},
    {150.0, 153.0, 156.0, 159.0, 162.0, 165.0, 168.0, 171.0, 174.0, 177.0, 180.0, 183.0, 186.0, 189.0, 192.0, 195.0, 198.0, 201.0, 204.0, 207.0, 210.0, 213.0, 216.0, 219.0, 222.0, 225.0, 228.0, 231.0, 234.0},
    {156.0, 159.0, 162.0, 165.0, 168.0, 171.0, 174.0, 177.0, 180.0, 183.0, 186.0, 189.0, 192.0, 195.0, 198.0, 201.0, 204.0, 207.0, 210.0, 213.0, 216.0, 219.0, 222.0, 225.0, 228.0, 231.0, 234.0, 237.0, 240.0},
    {162.0, 165.0, 168.0, 171.0, 174.0, 177.0, 180.0, 183.0, 186.0, 189.0, 192.0, 195.0, 198.0, 201.0, 204.0, 207.0, 210.0, 213.0, 216.0, 219.0, 222.0, 225.0, 228.0, 231.0, 234.0, 237.0, 240.0, 243.0, 246.0},
    {168.0, 171.0, 174.0, 177.0, 180.0, 183.0, 186.0, 189.0, 192.0, 195.0, 198.0, 201.0, 204.0, 207.0, 210.0, 213.0, 216.0, 219.0, 222.0, 225.0, 228.0, 231.0, 234.0, 237.0, 240.0, 243.0, 246.0, 249.0, 252.0},
    {174.0, 177.0, 180.0, 183.0, 186.0, 189.0, 192.0, 195.0, 198.0, 201.0, 204.0, 207.0, 210.0, 213.0, 216.0, 219.0, 222.0, 225.0, 228.0, 231.0, 234.0, 237.0, 240.0, 243.0, 246.0, 249.0, 252.0, 255.0, 258.0},
    {180.0, 183.0, 186.0, 189.0, 192.0, 195.0, 198.0, 201.0, 204.0, 207.0, 210.0, 213.0, 216.0, 219.0, 222.0, 225.0, 228.0, 231.0, 234.0, 237.0, 240.0, 243.0, 246.0, 249.0, 252.0, 255.0, 258.0, 261.0, 264.0},
    {186.0, 189.0, 192.0, 195.0, 198.0, 201.0, 204.0, 207.0, 210.0, 213.0, 216.0, 219.0, 222.0, 225.0, 228.0, 231.0, 234.0, 237.0, 240.0, 243.0, 246.0, 249.0, 252.0, 255.0, 258.0, 261.0, 264.0, 267.0, 270.0},
    {192.0, 195.0, 198.0, 201.0, 204.0, 207.0, 210.0, 213.0, 216.0, 219.0, 222.0, 225.0, 228.0, 231.0, 234.0, 237.0, 240.0, 243.0, 246.0, 249.0, 252.0, 255.0, 258.0, 261.0, 264.0, 267.0, 270.0, 273.0, 276.0},
    {198.0, 201.0, 204.0, 207.0, 210.0, 213.0, 216.0, 219.0, 222.0, 225.0, 228.0, 231.0, 234.0, 237.0, 240.0, 243.0, 246.0, 249.0, 252.0, 255.0, 258.0, 261.0, 264.0, 267.0, 270.0, 273.0, 276.0, 279.0, 282.0},
    {204.0, 207.0, 210.0, 213.0, 216.0, 219.0, 222.0, 225.0, 228.0, 231.0, 234.0, 237.0, 240.0, 243.0, 246.0, 249.0, 252.0, 255.0, 258.0, 261.0, 264.0, 267.0, 270.0, 273.0, 276.0, 279.0, 282.0, 285.0, 288.0},
    {210.0, 213.0, 216.0, 219.0, 222.0, 225.0, 228.0, 231.0, 234.0, 237.0, 240.0, 243.0, 246.0, 249.0, 252.0, 255.0, 258.0, 261.0, 264.0, 267.0, 270.0, 273.0, 276.0, 279.0, 282.0, 285.0, 288.0, 291.0, 294.0},
    {216.0, 219.0, 222.0, 225.0, 228.0, 231.0, 234.0, 237.0, 240.0, 243.0, 246.0, 249.0, 252.0, 255.0, 258.0, 261.0, 264.0, 267.0, 270.0, 273.0, 276.0, 279.0, 282.0, 285.0, 288.0, 291.0, 294.0, 297.0, 300.0},
    {222.0, 225.0, 228.0, 231.0, 234.0, 237.0, 240.0, 243.0, 246.0, 249.0, 252.0, 255.0, 258.0, 261.0, 264.0, 267.0, 270.0, 273.0, 276.0, 279.0, 282.0, 285.0, 288.0, 291.0, 294.0, 297.0, 300.0, 303.0, 306.0},
    {228.0, 231.0, 234.0, 237.0, 240.0, 243.0, 246.0, 249.0, 252.0, 255.0, 258.0, 261.0, 264.0, 267.0, 270.0, 273.0, 276.0, 279.0, 282.0, 285.0, 288.0, 291.0, 294.0, 297.0, 300.0, 303.0, 306.0, 309.0, 312.0},
    {234.0, 237.0, 240.0, 243.0, 246.0, 249.0, 252.0, 255.0, 258.0, 261.0, 264.0, 267.0, 270.0, 273.0, 276.0, 279.0, 282.0, 285.0, 288.0, 291.0, 294.0, 297.0, 300.0, 303.0, 306.0, 309.0, 312.0, 315.0, 318.0},
    {240.0, 243.0, 246.0, 249.0, 252.0, 255.0, 258.0, 261.0, 264.0, 267.0, 270.0, 273.0, 276.0, 279.0, 282.0, 285.0, 288.0, 291.0, 294.0, 297.0, 300.0, 303.0, 306.0, 309.0, 312.0, 315.0, 318.0, 321.0, 324.0},
    {246.0, 249.0, 252.0, 255.0, 258.0, 261.0, 264.0, 267.0, 270.0, 273.0, 276.0, 279.0, 282.0, 285.0, 288.0, 291.0, 294.0, 297.0, 300.0, 303.0, 306.0, 309.0, 312.0, 315.0, 318.0, 321.0, 324.0, 327.0, 330.0},
    {252.0, 255.0, 258.0, 261.0, 264.0, 267.0, 270.0, 273.0, 276.0, 279.0, 282.0, 285.0, 288.0, 291.0, 294.0, 297.0, 300.0, 303.0, 306.0, 309.0, 312.0, 315.0, 318.0, 321.0, 324.0, 327.0, 330.0, 333.0, 336.0},
    {258.0, 261.0, 264.0, 267.0, 270.0, 273.0, 276.0, 279.0, 282.0, 285.0, 288.0, 291.0, 294.0, 297.0, 300.0, 303.0, 306.0, 309.0, 312.0, 315.0, 318.0, 321.0, 324.0, 327.0, 330.0, 333.0, 336.0, 339.0, 342.0},
    {264.0, 267.0, 270.0, 273.0, 276.0, 279.0, 282.0, 285.0, 288.0, 291.0, 294.0, 297.0, 300.0, 303.0, 306.0, 309.0, 312.0, 315.0, 318.0, 321.0, 324.0, 327.0, 330.0, 333.0, 336.0, 339.0, 342.0, 345.0, 348.0},
    {270.0, 273.0, 276.0, 279.0, 282.0, 285.0, 288.0, 291.0, 294.0, 297.0, 300.0, 303.0, 306.0, 309.0, 312.0, 315.0, 318.0, 321.0, 324.0, 327.0, 330.0, 333.0, 336.0, 339.0, 342.0, 345.0, 348.0, 351.0, 354.0},
    {276.0, 279.0, 282.0, 285.0, 288.0, 291.0, 294.0, 297.0, 300.0, 303.0, 306.0, 309.0, 312.0, 315.0, 318.0, 321.0, 324.0, 327.0, 330.0, 333.0, 336.0, 339.0, 342.0, 345.0, 348.0, 351.0, 354.0, 357.0, 360.0},
    {282.0, 285.0, 288.0, 291.0, 294.0, 297.0, 300.0, 303.0, 306.0, 309.0, 312.0, 315.0, 318.0, 321.0, 324.0, 327.0, 330.0, 333.0, 336.0, 339.0, 342.0, 345.0, 348.0, 351.0, 354.0, 357.0, 360.0, 363.0, 366.0},
    {288.0, 291.0, 294.0, 297.0, 300.0, 303.0, 306.0, 309.0, 312.0, 315.0, 318.0, 321.0, 324.0, 327.0, 330.0, 333.0, 336.0, 339.0, 342.0, 345.0, 348.0, 351.0, 354.0, 357.0, 360.0, 363.0, 366.0, 369.0, 372.0},
    {294.0, 297.0, 300.0, 303.0, 306.0, 309.0, 312.0, 315.0, 318.0, 321.0, 324.0, 327.0, 330.0, 333.0, 336.0, 339.0, 342.0, 345.0, 348.0, 351.0, 354.0, 357.0, 360.0, 363.0, 366.0, 369.0, 372.0, 375.0, 378.0},
    {300.0, 303.0, 306.0, 309.0, 312.0, 315.0, 318.0, 321.0, 324.0, 327.0, 330.0, 333.0, 336.0, 339.0, 342.0, 345.0, 348.0, 351.0, 354.0, 357.0, 360.0, 363.0, 366.0, 369.0, 372.0, 375.0, 378.0, 381.0, 384.0},
    {306.0, 309.0, 312.0, 315.0, 318.0, 321.0, 324.0, 327.0, 330.0, 333.0, 336.0, 339.0, 342.0, 345.0, 348.0, 351.0, 354.0, 357.0, 360.0, 363.0, 366.0, 369.0, 372.0, 375.0, 378.0, 381.0, 384.0, 387.0, 390.0},
    {312.0, 315.0, 318.0, 321.0, 324.0, 327.0, 330.0, 333.0, 336.0, 339.0, 342.0, 345.0, 348.0, 351.0, 354.0, 357.0, 360.0, 363.0, 366.0, 369.0, 372.0, 375.0, 378.0, 381.0, 384.0, 387.0, 390.0, 393.0, 396.0},
    {318.0, 321.0, 324.0, 327.0, 330.0, 333.0, 336.0, 339.0, 342.0, 345.0, 348.0, 351.0, 354.0, 357.0, 360.0, 363.0, 366.0, 369.0, 372.0, 375.0, 378.0, 381.0, 384.0, 387.0, 390.0, 393.0, 396.0, 399.0, 402.0},
    {324.0, 327.0, 330.0, 333.0, 336.0, 339.0, 342.0, 345.0, 348.0, 351.0, 354.0, 357.0, 360.0, 363.0, 366.0, 369.0, 372.0, 375.0, 378.0, 381.0, 384.0, 387.0, 390.0, 393.0, 396.0, 399.0, 402.0, 405.0, 408.0},
    {330.0, 333.0, 336.0, 339.0, 342.0, 345.0, 348.0, 351.0, 354.0, 357.0, 360.0, 363.0, 366.0, 369.0, 372.0, 375.0, 378.0, 381.0, 384.0, 387.0, 390.0, 393.0, 396.0, 399.0, 402.0, 405.0, 408.0, 411.0, 414.0},
    {336.0, 339.0, 342.0, 345.0, 348.0, 351.0, 354.0, 357.0, 360.0, 363.0, 366.0, 369.0, 372.0, 375.0, 378.0, 381.0, 384.0, 387.0, 390.0, 393.0, 396.0, 399.0, 402.0, 405.0, 408.0, 411.0, 414.0, 417.0, 420.0},
    {342.0, 345.0, 348.0, 351.0, 354.0, 357.0, 360.0, 363.0, 366.0, 369.0, 372.0, 375.0, 378.0, 381.0, 384.0, 387.0, 390.0, 393.0, 396.0, 399.0, 402.0, 405.0, 408.0, 411.0, 414.0, 417.0, 420.0, 423.0, 426.0},
    {348.0, 351.0, 354.0, 357.0, 360.0, 363.0, 366.0, 369.0, 372.0, 375.0, 378.0, 381.0, 384.0, 387.0, 390.0, 393.0, 396.0, 399.0, 402.0, 405.0, 408.0, 411.0, 414.0, 417.0, 420.0, 423.0, 426.0, 429.0, 432.0},
    {354.0, 357.0, 360.0, 363.0, 366.0, 369.0, 372.0, 375.0, 378.0, 381.0, 384.0, 387.0, 390.0, 393.0, 396.0, 399.0, 402.0, 405.0, 408.0, 411.0, 414.0, 417.0, 420.0, 423.0, 426.0, 429.0, 432.0, 435.0, 438.0},
    {360.0, 363.0, 366.0, 369.0, 372.0, 375.0, 378.0, 381.0, 384.0, 387.0, 390.0, 393.0, 396.0, 399.0, 402.0, 405.0, 408.0, 411.0, 414.0, 417.0, 420.0, 423.0, 426.0, 429.0, 432.0, 435.0, 438.0, 441.0, 444.0},
    {366.0, 369.0, 372.0, 375.0, 378.0, 381.0, 384.0, 387.0, 390.0, 393.0, 396.0, 399.0, 402.0, 405.0, 408.0, 411.0, 414.0, 417.0, 420.0, 423.0, 426.0, 429.0, 432.0, 435.0, 438.0, 441.0, 444.0, 447.0, 450.0},
    {372.0, 375.0, 378.0, 381.0, 384.0, 387.0, 390.0, 393.0, 396.0, 399.0, 402.0, 405.0, 408.0, 411.0, 414.0, 417.0, 420.0, 423.0, 426.0, 429.0, 432.0, 435.0, 438.0, 441.0, 444.0, 447.0, 450.0, 453.0, 456.0},
    {378.0, 381.0, 384.0, 387.0, 390.0, 393.0, 396.0, 399.0, 402.0, 405.0, 408.0, 411.0, 414.0, 417.0, 420.0, 423.0, 426.0, 429.0, 432.0, 435.0, 438.0, 441.0, 444.0, 447.0, 450.0, 453.0, 456.0, 459.0, 462.0},
    {384.0, 387.0, 390.0, 393.0, 396.0, 399.0, 402.0, 405.0, 408.0, 411.0, 414.0, 417.0, 420.0, 423.0, 426.0, 429.0, 432.0, 435.0, 438.0, 441.0, 444.0, 447.0, 450.0, 453.0, 456.0, 459.0, 462.0, 465.0, 468.0},
    {390.0, 393.0, 396.0, 399.0, 402.0, 405.0, 408.0, 411.0, 414.0, 417.0, 420.0, 423.0, 426.0, 429.0, 432.0, 435.0, 438.0, 441.0, 444.0, 447.0, 450.0, 453.0, 456.0, 459.0, 462.0, 465.0, 468.0, 471.0, 474.0},
    {396.0, 399.0, 402.0, 405.0, 408.0, 411.0, 414.0, 417.0, 420.0, 423.0, 426.0, 429.0, 432.0, 435.0, 438.0, 441.0, 444.0, 447.0, 450.0, 453.0, 456.0, 459.0, 462.0, 465.0, 468.0, 471.0, 474.0, 477.0, 480.0},
    {402.0, 405.0, 408.0, 411.0, 414.0, 417.0, 420.0, 423.0, 426.0, 429.0, 432.0, 435.0, 438.0, 441.0, 444.0, 447.0, 450.0, 453.0, 456.0, 459.0, 462.0, 465.0, 468.0, 471.0, 474.0, 477.0, 480.0, 483.0, 486.0},
    {408.0, 411.0, 414.0, 417.0, 420.0, 423.0, 426.0, 429.0, 432.0, 435.0, 438.0, 441.0, 444.0, 447.0, 450.0, 453.0, 456.0, 459.0, 462.0, 465.0, 468.0, 471.0, 474.0, 477.0, 480.0, 483.0, 486.0, 489.0, 492.0},
    {414.0, 417.0, 420.0, 423.0, 426.0, 429.0, 432.0, 435.0, 438.0, 441.0, 444.0, 447.0, 450.0, 453.0, 456.0, 459.0, 462.0, 465.0, 468.0, 471.0, 474.0, 477.0, 480.0, 483.0, 486.0, 489.0, 492.0, 495.0, 498.0},
    {420.0, 423.0, 426.0, 429.0, 432.0, 435.0, 438.0, 441.0, 444.0, 447.0, 450.0, 453.0, 456.0, 459.0, 462.0, 465.0, 468.0, 471.0, 474.0, 477.0, 480.0, 483.0, 486.0, 489.0, 492.0, 495.0, 498.0, 501.0, 504.0},
    {426.0, 429.0, 432.0, 435.0, 438.0, 441.0, 444.0, 447.0, 450.0, 453.0, 456.0, 459.0, 462.0, 465.0, 468.0, 471.0, 474.0, 477.0, 480.0, 483.0, 486.0, 489.0, 492.0, 495.0, 498.0, 501.0, 504.0, 507.0, 510.0},
    {432.0, 435.0, 438.0, 441.0, 444.0, 447.0, 450.0, 453.0, 456.0, 459.0, 462.0, 465.0, 468.0, 471.0, 474.0, 477.0, 480.0, 483.0, 486.0, 489.0, 492.0, 495.0, 498.0, 501.0, 504.0, 507.0, 510.0, 513.0, 516.0},
    {438.0, 441.0, 444.0, 447.0, 450.0, 453.0, 456.0, 459.0, 462.0, 465.0, 468.0, 471.0, 474.0, 477.0, 480.0, 483.0, 486.0, 489.0, 492.0, 495.0, 498.0, 501.0, 504.0, 507.0, 510.0, 513.0, 516.0, 519.0, 522.0},
    {444.0, 447.0, 450.0, 453.0, 456.0, 459.0, 462.0, 465.0, 468.0, 471.0, 474.0, 477.0, 480.0, 483.0, 486.0, 489.0, 492.0, 495.0, 498.0, 501.0, 504.0, 507.0, 510.0, 513.0, 516.0, 519.0, 522.0, 525.0, 528.0},
    {450.0, 453.0, 456.0, 459.0, 462.0, 465.0, 468.0, 471.0, 474.0, 477.0, 480.0, 483.0, 486.0, 489.0, 492.0, 495.0, 498.0, 501.0, 504.0, 507.0, 510.0, 513.0, 516.0, 519.0, 522.0, 525.0, 528.0, 531.0, 534.0},
    {456.0, 459.0, 462.0, 465.0, 468.0, 471.0, 474.0, 477.0, 480.0, 483.0, 486.0, 489.0, 492.0, 495.0, 498.0, 501.0, 504.0, 507.0, 510.0, 513.0, 516.0, 519.0, 522.0, 525.0, 528.0, 531.0, 534.0, 537.0, 540.0},
    {462.0, 465.0, 468.0, 471.0, 474.0, 477.0, 480.0, 483.0, 486.0, 489.0, 492.0, 495.0, 498.0, 501.0, 504.0, 507.0, 510.0, 513.0, 516.0, 519.0, 522.0, 525.0, 528.0, 531.0, 534.0, 537.0, 540.0, 543.0, 546.0},
    {468.0, 471.0, 474.0, 477.0, 480.0, 483.0, 486.0, 489.0, 492.0, 495.0, 498.0, 501.0, 504.0, 507.0, 510.0, 513.0, 516.0, 519.0, 522.0, 525.0, 528.0, 531.0, 534.0, 537.0, 540.0, 543.0, 546.0, 549.0, 552.0},
    {474.0, 477.0, 480.0, 483.0, 486.0, 489.0, 492.0, 495.0, 498.0, 501.0, 504.0, 507.0, 510.0, 513.0, 516.0, 519.0, 522.0, 525.0, 528.0, 531.0, 534.0, 537.0, 540.0, 543.0, 546.0, 549.0, 552.0, 555.0, 558.0},
    {480.0, 483.0, 486.0, 489.0, 492.0, 495.0, 498.0, 501.0, 504.0, 507.0, 510.0, 513.0, 516.0, 519.0, 522.0, 525.0, 528.0, 531.0, 534.0, 537.0, 540.0, 543.0, 546.0, 549.0, 552.0, 555.0, 558.0, 561.0, 564.0},
    {486.0, 489.0, 492.0, 495.0, 498.0, 501.0, 504.0, 507.0, 510.0, 513.0, 516.0, 519.0, 522.0, 525.0, 528.0, 531.0, 534.0, 537.0, 540.0, 543.0, 546.0, 549.0, 552.0, 555.0, 558.0, 561.0, 564.0, 567.0, 570.0},
    {492.0, 495.0, 498.0, 501.0, 504.0, 507.0, 510.0, 513.0, 516.0, 519.0, 522.0, 525.0, 528.0, 531.0, 534.0, 537.0, 540.0, 543.0, 546.0, 549.0, 552.0, 555.0, 558.0, 561.0, 564.0, 567.0, 570.0, 573.0, 576.0},
    {498.0, 501.0, 504.0, 507.0, 510.0, 513.0, 516.0, 519.0, 522.0, 525.0, 528.0, 531.0, 534.0, 537.0, 540.0, 543.0, 546.0, 549.0, 552.0, 555.0, 558.0, 561.0, 564.0, 567.0, 570.0, 573.0, 576.0, 579.0, 582.0},
    {504.0, 507.0, 510.0, 513.0, 516.0, 519.0, 522.0, 525.0, 528.0, 531.0, 534.0, 537.0, 540.0, 543.0, 546.0, 549.0, 552.0, 555.0, 558.0, 561.0, 564.0, 567.0, 570.0, 573.0, 576.0, 579.0, 582.0, 585.0, 588.0},
    {510.0, 513.0, 516.0, 519.0, 522.0, 525.0, 528.0, 531.0, 534.0, 537.0, 540.0, 543.0, 546.0, 549.0, 552.0, 555.0, 558.0, 561.0, 564.0, 567.0, 570.0, 573.0, 576.0, 579.0, 582.0, 585.0, 588.0, 591.0, 594.0},
    {516.0, 519.0, 522.0, 525.0, 528.0, 531.0, 534.0, 537.0, 540.0, 543.0, 546.0, 549.0, 552.0, 555.0, 558.0, 561.0, 564.0, 567.0, 570.0, 573.0, 576.0, 579.0, 582.0, 585.0, 588.0, 591.0, 594.0, 597.0, 600.0},
    {522.0, 525.0, 528.0, 531.0, 534.0, 537.0, 540.0, 543.0, 546.0, 549.0, 552.0, 555.0, 558.0, 561.0, 564.0, 567.0, 570.0, 573.0, 576.0, 579.0, 582.0, 585.0, 588.0, 591.0, 594.0, 597.0, 600.0, 603.0, 606.0},
    {528.0, 531.0, 534.0, 537.0, 540.0, 543.0, 546.0, 549.0, 552.0, 555.0, 558.0, 561.0, 564.0, 567.0, 570.0, 573.0, 576.0, 579.0, 582.0, 585.0, 588.0, 591.0, 594.0, 597.0, 600.0, 603.0, 606.0, 609.0, 612.0},
    {534.0, 537.0, 540.0, 543.0, 546.0, 549.0, 552.0, 555.0, 558.0, 561.0, 564.0, 567.0, 570.0, 573.0, 576.0, 579.0, 582.0, 585.0, 588.0, 591.0, 594.0, 597.0, 600.0, 603.0, 606.0, 609.0, 612.0, 615.0, 618.0},
    {540.0, 543.0, 546.0, 549.0, 552.0, 555.0, 558.0, 561.0, 564.0, 567.0, 570.0, 573.0, 576.0, 579.0, 582.0, 585.0, 588.0, 591.0, 594.0, 597.0, 600.0, 603.0, 606.0, 609.0, 612.0, 615.0, 618.0, 621.0, 624.0},
    {546.0, 549.0, 552.0, 555.0, 558.0, 561.0, 564.0, 567.0, 570.0, 573.0, 576.0, 579.0, 582.0, 585.0, 588.0, 591.0, 594.0, 597.0, 600.0, 603.0, 606.0, 609.0, 612.0, 615.0, 618.0, 621.0, 624.0, 627.0, 630.0},
    {552.0, 555.0, 558.0, 561.0, 564.0, 567.0, 570.0, 573.0, 576.0, 579.0, 582.0, 585.0, 588.0, 591.0, 594.0, 597.0, 600.0, 603.0, 606.0, 609.0, 612.0, 615.0, 618.0, 621.0, 624.0, 627.0, 630.0, 633.0, 636.0},
    {558.0, 561.0, 564.0, 567.0, 570.0, 573.0, 576.0, 579.0, 582.0, 585.0, 588.0, 591.0, 594.0, 597.0, 600.0, 603.0, 606.0, 609.0, 612.0, 615.0, 618.0, 621.0, 624.0, 627.0, 630.0, 633.0, 636.0, 639.0, 642.0},
    {564.0, 567.0, 570.0, 573.0, 576.0, 579.0, 582.0, 585.0, 588.0, 591.0, 594.0, 597.0, 600.0, 603.0, 606.0, 609.0, 612.0, 615.0, 618.0, 621.0, 624.0, 627.0, 630.0, 633.0, 636.0, 639.0, 642.0, 645.0, 648.0},
    {570.0, 573.0, 576.0, 579.0, 582.0, 585.0, 588.0, 591.0, 594.0, 597.0, 600.0, 603.0, 606.0, 609.0, 612.0, 615.0, 618.0, 621.0, 624.0, 627.0, 630.0, 633.0, 636.0, 639.0, 642.0, 645.0, 648.0, 651.0, 654.0},
    {576.0, 579.0, 582.0, 585.0, 588.0, 591.0, 594.0, 597.0, 600.0, 603.0, 606.0, 609.0, 612.0, 615.0, 618.0, 621.0, 624.0, 627.0, 630.0, 633.0, 636.0, 639.0, 642.0, 645.0, 648.0, 651.0, 654.0, 657.0, 660.0},
    {582.0, 585.0, 588.0, 591.0, 594.0, 597.0, 600.0, 603.0, 606.0, 609.0, 612.0, 615.0, 618.0, 621.0, 624.0, 627.0, 630.0, 633.0, 636.0, 639.0, 642.0, 645.0, 648.0, 651.0, 654.0, 657.0, 660.0, 663.0, 666.0},
    {588.0, 591.0, 594.0, 597.0, 600.0, 603.0, 606.0, 609.0, 612.0, 615.0, 618.0, 621.0, 624.0, 627.0, 630.0, 633.0, 636.0, 639.0, 642.0, 645.0, 648.0, 651.0, 654.0, 657.0, 660.0, 663.0, 666.0, 669.0, 672.0},
    {594.0, 597.0, 600.0, 603.0, 606.0, 609.0, 612.0, 615.0, 618.0, 621.0, 624.0, 627.0, 630.0, 633.0, 636.0, 639.0, 642.0, 645.0, 648.0, 651.0, 654.0, 657.0, 660.0, 663.0, 666.0, 669.0, 672.0, 675.0, 678.0},
    {600.0, 603.0, 606.0, 609.0, 612.0, 615.0, 618.0, 621.0, 624.0, 627.0, 630.0, 633.0, 636.0, 639.0, 642.0, 645.0, 648.0, 651.0, 654.0, 657.0, 660.0, 663.0, 666.0, 669.0, 672.0, 675.0, 678.0, 681.0, 684.0},
    {606.0, 609.0, 612.0, 615.0, 618.0, 621.0, 624.0, 627.0, 630.0, 633.0, 636.0, 639.0, 642.0, 645.0, 648.0, 651.0, 654.0, 657.0, 660.0, 663.0, 666.0, 669.0, 672.0, 675.0, 678.0, 681.0, 684.0, 687.0, 690.0},
    {612.0, 615.0, 618.0, 621.0, 624.0, 627.0, 630.0, 633.0, 636.0, 639.0, 642.0, 645.0, 648.0, 651.0, 654.0, 657.0, 660.0, 663.0, 666.0, 669.0, 672.0, 675.0, 678.0, 681.0, 684.0, 687.0, 690.0, 693.0, 696.0},
    {618.0, 621.0, 624.0, 627.0, 630.0, 633.0, 636.0, 639.0, 642.0, 645.0, 648.0, 651.0, 654.0, 657.0, 660.0, 663.0, 666.0, 669.0, 672.0, 675.0, 678.0, 681.0, 684.0, 687.0, 690.0, 693.0, 696.0, 699.0, 702.0},
    {624.0, 627.0, 630.0, 633.0, 636.0, 639.0, 642.0, 645.0, 648.0, 651.0, 654.0, 657.0, 660.0, 663.0, 666.0, 669.0, 672.0, 675.0, 678.0, 681.0, 684.0, 687.0, 690.0, 693.0, 696.0, 699.0, 702.0, 705.0, 708.0},
    {630.0, 633.0, 636.0, 639.0, 642.0, 645.0, 648.0, 651.0, 654.0, 657.0, 660.0, 663.0, 666.0, 669.0, 672.0, 675.0, 678.0, 681.0, 684.0, 687.0, 690.0, 693.0, 696.0, 699.0, 702.0, 705.0, 708.0, 711.0, 714.0},
    {636.0, 639.0, 642.0, 645.0, 648.0, 651.0, 654.0, 657.0, 660.0, 663.0, 666.0, 669.0, 672.0, 675.0, 678.0, 681.0, 684.0, 687.0, 690.0, 693.0, 696.0, 699.0, 702.0, 705.0, 708.0, 711.0, 714.0, 717.0, 720.0},
    {642.0, 645.0, 648.0, 651.0, 654.0, 657.0, 660.0, 663.0, 666.0, 669.0, 672.0, 675.0, 678.0, 681.0, 684.0, 687.0, 690.0, 693.0, 696.0, 699.0, 702.0, 705.0, 708.0, 711.0, 714.0, 717.0, 720.0, 723.0, 726.0},
    {648.0, 651.0, 654.0, 657.0, 660.0, 663.0, 666.0, 669.0, 672.0, 675.0, 678.0, 681.0, 684.0, 687.0, 690.0, 693.0, 696.0, 699.0, 702.0, 705.0, 708.0, 711.0, 714.0, 717.0, 720.0, 723.0, 726.0, 729.0, 732.0},
    {654.0, 657.0, 660.0, 663.0, 666.0, 669.0, 672.0, 675.0, 678.0, 681.0, 684.0, 687.0, 690.0, 693.0, 696.0, 699.0, 702.0, 705.0, 708.0, 711.0, 714.0, 717.0, 720.0, 723.0, 726.0, 729.0, 732.0, 735.0, 738.0},
    {660.0, 663.0, 666.0, 669.0, 672.0, 675.0, 678.0, 681.0, 684.0, 687.0, 690.0, 693.0, 696.0, 699.0, 702.0, 705.0, 708.0, 711.0, 714.0, 717.0, 720.0, 723.0, 726.0, 729.0, 732.0, 735.0, 738.0, 741.0, 744.0},
    {666.0, 669.0, 672.0, 675.0, 678.0, 681.0, 684.0, 687.0, 690.0, 693.0, 696.0, 699.0, 702.0, 705.0, 708.0, 711.0, 714.0, 717.0, 720.0, 723.0, 726.0, 729.0, 732.0, 735.0, 738.0, 741.0, 744.0, 747.0, 750.0},
    {672.0, 675.0, 678.0, 681.0, 684.0, 687.0, 690.0, 693.0, 696.0, 699.0, 702.0, 705.0, 708.0, 711.0, 714.0, 717.0, 720.0, 723.0, 726.0, 729.0, 732.0, 735.0, 738.0, 741.0, 744.0, 747.0, 750.0, 753.0, 756.0},
    {678.0, 681.0, 684.0, 687.0, 690.0, 693.0, 696.0, 699.0, 702.0, 705.0, 708.0, 711.0, 714.0, 717.0, 720.0, 723.0, 726.0, 729.0, 732.0, 735.0, 738.0, 741.0, 744.0, 747.0, 750.0, 753.0, 756.0, 759.0, 762.0},
    {684.0, 687.0, 690.0, 693.0, 696.0, 699.0, 702.0, 705.0, 708.0, 711.0, 714.0, 717.0, 720.0, 723.0, 726.0, 729.0, 732.0, 735.0, 738.0, 741.0, 744.0, 747.0, 750.0, 753.0, 756.0, 759.0, 762.0, 765.0, 768.0},
    {690.0, 693.0, 696.0, 699.0, 702.0, 705.0, 708.0, 711.0, 714.0, 717.0, 720.0, 723.0, 726.0, 729.0, 732.0, 735.0, 738.0, 741.0, 744.0, 747.0, 750.0, 753.0, 756.0, 759.0, 762.0, 765.0, 768.0, 771.0, 774.0},
    {696.0, 699.0, 702.0, 705.0, 708.0, 711.0, 714.0, 717.0, 720.0, 723.0, 726.0, 729.0, 732.0, 735.0, 738.0, 741.0, 744.0, 747.0, 750.0, 753.0, 756.0, 759.0, 762.0, 765.0, 768.0, 771.0, 774.0, 777.0, 780.0},
    {702.0, 705.0, 708.0, 711.0, 714.0, 717.0, 720.0, 723.0, 726.0, 729.0, 732.0, 735.0, 738.0, 741.0, 744.0, 747.0, 750.0, 753.0, 756.0, 759.0, 762.0, 765.0, 768.0, 771.0, 774.0, 777.0, 780.0, 783.0, 786.0},
    {708.0, 711.0, 714.0, 717.0, 720.0, 723.0, 726.0, 729.0, 732.0, 735.0, 738.0, 741.0, 744.0, 747.0, 750.0, 753.0, 756.0, 759.0, 762.0, 765.0, 768.0, 771.0, 774.0, 777.0, 780.0, 783.0, 786.0, 789.0, 792.0},
    {714.0, 717.0, 720.0, 723.0, 726.0, 729.0, 732.0, 735.0, 738.0, 741.0, 744.0, 747.0, 750.0, 753.0, 756.0, 759.0, 762.0, 765.0, 768.0, 771.0, 774.0, 777.0, 780.0, 783.0, 786.0, 789.0, 792.0, 795.0, 798.0},
    {720.0, 723.0, 726.0, 729.0, 732.0, 735.0, 738.0, 741.0, 744.0, 747.0, 750.0, 753.0, 756.0, 759.0, 762.0, 765.0, 768.0, 771.0, 774.0, 777.0, 780.0, 783.0, 786.0, 789.0, 792.0, 795.0, 798.0, 801.0, 804.0},
    {726.0, 729.0, 732.0, 735.0, 738.0, 741.0, 744.0, 747.0, 750.0, 753.0, 756.0, 759.0, 762.0, 765.0, 768.0, 771.0, 774.0, 777.0, 780.0, 783.0, 786.0, 789.0, 792.0, 795.0, 798.0, 801.0, 804.0, 807.0, 810.0},
    {732.0, 735.0, 738.0, 741.0, 744.0, 747.0, 750.0, 753.0, 756.0, 759.0, 762.0, 765.0, 768.0, 771.0, 774.0, 777.0, 780.0, 783.0, 786.0, 789.0, 792.0, 795.0, 798.0, 801.0, 804.0, 807.0, 810.0, 813.0, 816.0},
    {738.0, 741.0, 744.0, 747.0, 750.0, 753.0, 756.0, 759.0, 762.0, 765.0, 768.0, 771.0, 774.0, 777.0, 780.0, 783.0, 786.0, 789.0, 792.0, 795.0, 798.0, 801.0, 804.0, 807.0, 810.0, 813.0, 816.0, 819.0, 822.0},
    {744.0, 747.0, 750.0, 753.0, 756.0, 759.0, 762.0, 765.0, 768.0, 771.0, 774.0, 777.0, 780.0, 783.0, 786.0, 789.0, 792.0, 795.0, 798.0, 801.0, 804.0, 807.0, 810.0, 813.0, 816.0, 819.0, 822.0, 825.0, 828.0},
    {750.0, 753.0, 756.0, 759.0, 762.0, 765.0, 768.0, 771.0, 774.0, 777.0, 780.0, 783.0, 786.0, 789.0, 792.0, 795.0, 798.0, 801.0, 804.0, 807.0, 810.0, 813.0, 816.0, 819.0, 822.0, 825.0, 828.0, 831.0, 834.0},
    {756.0, 759.0, 762.0, 765.0, 768.0, 771.0, 774.0, 777.0, 780.0, 783.0, 786.0, 789.0, 792.0, 795.0, 798.0, 801.0, 804.0, 807.0, 810.0, 813.0, 816.0, 819.0, 822.0, 825.0, 828.0, 831.0, 834.0, 837.0, 840.0},
    {762.0, 765.0, 768.0, 771.0, 774.0, 777.0, 780.0, 783.0, 786.0, 789.0, 792.0, 795.0, 798.0, 801.0, 804.0, 807.0, 810.0, 813.0, 816.0, 819.0, 822.0, 825.0, 828.0, 831.0, 834.0, 837.0, 840.0, 843.0, 846.0},
    {768.0, 771.0, 774.0, 777.0, 780.0, 783.0, 786.0, 789.0, 792.0, 795.0, 798.0, 801.0, 804.0, 807.0, 810.0, 813.0, 816.0, 819.0, 822.0, 825.0, 828.0, 831.0, 834.0, 837.0, 840.0, 843.0, 846.0, 849.0, 852.0},
    {774.0, 777.0, 780.0, 783.0, 786.0, 789.0, 792.0, 795.0, 798.0, 801.0, 804.0, 807.0, 810.0, 813.0, 816.0, 819.0, 822.0, 825.0, 828.0, 831.0, 834.0, 837.0, 840.0, 843.0, 846.0, 849.0, 852.0, 855.0, 858.0},
    {780.0, 783.0, 786.0, 789.0, 792.0, 795.0, 798.0, 801.0, 804.0, 807.0, 810.0, 813.0, 816.0, 819.0, 822.0, 825.0, 828.0, 831.0, 834.0, 837.0, 840.0, 843.0, 846.0, 849.0, 852.0, 855.0, 858.0, 861.0, 864.0},
    {786.0, 789.0, 792.0, 795.0, 798.0, 801.0, 804.0, 807.0, 810.0, 813.0, 816.0, 819.0, 822.0, 825.0, 828.0, 831.0, 834.0, 837.0, 840.0, 843.0, 846.0, 849.0, 852.0, 855.0, 858.0, 861.0, 864.0, 867.0, 870.0},
    {792.0, 795.0, 798.0, 801.0, 804.0, 807.0, 810.0, 813.0, 816.0, 819.0, 822.0, 825.0, 828.0, 831.0, 834.0, 837.0, 840.0, 843.0, 846.0, 849.0, 852.0, 855.0, 858.0, 861.0, 864.0, 867.0, 870.0, 873.0, 876.0},
    {798.0, 801.0, 804.0, 807.0, 810.0, 813.0, 816.0, 819.0, 822.0, 825.0, 828.0, 831.0, 834.0, 837.0, 840.0, 843.0, 846.0, 849.0, 852.0, 855.0, 858.0, 861.0, 864.0, 867.0, 870.0, 873.0, 876.0, 879.0, 882.0},
    {804.0, 807.0, 810.0, 813.0, 816.0, 819.0, 822.0, 825.0, 828.0, 831.0, 834.0, 837.0, 840.0, 843.0, 846.0, 849.0, 852.0, 855.0, 858.0, 861.0, 864.0, 867.0, 870.0, 873.0, 876.0, 879.0, 882.0, 885.0, 888.0},
    {810.0, 813.0, 816.0, 819.0, 822.0, 825.0, 828.0, 831.0, 834.0, 837.0, 840.0, 843.0, 846.0, 849.0, 852.0, 855.0, 858.0, 861.0, 864.0, 867.0, 870.0, 873.0, 876.0, 879.0, 882.0, 885.0, 888.0, 891.0, 894.0},
    {816.0, 819.0, 822.0, 825.0, 828.0, 831.0, 834.0, 837.0, 840.0, 843.0, 846.0, 849.0, 852.0, 855.0, 858.0, 861.0, 864.0, 867.0, 870.0, 873.0, 876.0, 879.0, 882.0, 885.0, 888.0, 891.0, 894.0, 897.0, 900.0},
    {822.0, 825.0, 828.0, 831.0, 834.0, 837.0, 840.0, 843.0, 846.0, 849.0, 852.0, 855.0, 858.0, 861.0, 864.0, 867.0, 870.0, 873.0, 876.0, 879.0, 882.0, 885.0, 888.0, 891.0, 894.0, 897.0, 900.0, 903.0, 906.0},
    {828.0, 831.0, 834.0, 837.0, 840.0, 843.0, 846.0, 849.0, 852.0, 855.0, 858.0, 861.0, 864.0, 867.0, 870.0, 873.0, 876.0, 879.0, 882.0, 885.0, 888.0, 891.0, 894.0, 897.0, 900.0, 903.0, 906.0, 909.0, 912.0},
    {834.0, 837.0, 840.0, 843.0, 846.0, 849.0, 852.0, 855.0, 858.0, 861.0, 864.0, 867.0, 870.0, 873.0, 876.0, 879.0, 882.0, 885.0, 888.0, 891.0, 894.0, 897.0, 900.0, 903.0, 906.0, 909.0, 912.0, 915.0, 918.0},
    {840.0, 843.0, 846.0, 849.0, 852.0, 855.0, 858.0, 861.0, 864.0, 867.0, 870.0, 873.0, 876.0, 879.0, 882.0, 885.0, 888.0, 891.0, 894.0, 897.0, 900.0, 903.0, 906.0, 909.0, 912.0, 915.0, 918.0, 921.0, 924.0},
    {846.0, 849.0, 852.0, 855.0, 858.0, 861.0, 864.0, 867.0, 870.0, 873.0, 876.0, 879.0, 882.0, 885.0, 888.0, 891.0, 894.0, 897.0, 900.0, 903.0, 906.0, 909.0, 912.0, 915.0, 918.0, 921.0, 924.0, 927.0, 930.0},
    {852.0, 855.0, 858.0, 861.0, 864.0, 867.0, 870.0, 873.0, 876.0, 879.0, 882.0, 885.0, 888.0, 891.0, 894.0, 897.0, 900.0, 903.0, 906.0, 909.0, 912.0, 915.0, 918.0, 921.0, 924.0, 927.0, 930.0, 933.0, 936.0},
    {858.0, 861.0, 864.0, 867.0, 870.0, 873.0, 876.0, 879.0, 882.0, 885.0, 888.0, 891.0, 894.0, 897.0, 900.0, 903.0, 906.0, 909.0, 912.0, 915.0, 918.0, 921.0, 924.0, 927.0, 930.0, 933.0, 936.0, 939.0, 942.0},
    {864.0, 867.0, 870.0, 873.0, 876.0, 879.0, 882.0, 885.0, 888.0, 891.0, 894.0, 897.0, 900.0, 903.0, 906.0, 909.0, 912.0, 915.0, 918.0, 921.0, 924.0, 927.0, 930.0, 933.0, 936.0, 939.0, 942.0, 945.0, 948.0},
    {870.0, 873.0, 876.0, 879.0, 882.0, 885.0, 888.0, 891.0, 894.0, 897.0, 900.0, 903.0, 906.0, 909.0, 912.0, 915.0, 918.0, 921.0, 924.0, 927.0, 930.0, 933.0, 936.0, 939.0, 942.0, 945.0, 948.0, 951.0, 954.0},
    {876.0, 879.0, 882.0, 885.0, 888.0, 891.0, 894.0, 897.0, 900.0, 903.0, 906.0, 909.0, 912.0, 915.0, 918.0, 921.0, 924.0, 927.0, 930.0, 933.0, 936.0, 939.0, 942.0, 945.0, 948.0, 951.0, 954.0, 957.0, 960.0},
    {882.0, 885.0, 888.0, 891.0, 894.0, 897.0, 900.0, 903.0, 906.0, 909.0, 912.0, 915.0, 918.0, 921.0, 924.0, 927.0, 930.0, 933.0, 936.0, 939.0, 942.0, 945.0, 948.0, 951.0, 954.0, 957.0, 960.0, 963.0, 966.0},
    {888.0, 891.0, 894.0, 897.0, 900.0, 903.0, 906.0, 909.0, 912.0, 915.0, 918.0, 921.0, 924.0, 927.0, 930.0, 933.0, 936.0, 939.0, 942.0, 945.0, 948.0, 951.0, 954.0, 957.0, 960.0, 963.0, 966.0, 969.0, 972.0},
    {894.0, 897.0, 900.0, 903.0, 906.0, 909.0, 912.0, 915.0, 918.0, 921.0, 924.0, 927.0, 930.0, 933.0, 936.0, 939.0, 942.0, 945.0, 948.0, 951.0, 954.0, 957.0, 960.0, 963.0, 966.0, 969.0, 972.0, 975.0, 978.0},
    {900.0, 903.0, 906.0, 909.0, 912.0, 915.0, 918.0, 921.0, 924.0, 927.0, 930.0, 933.0, 936.0, 939.0, 942.0, 945.0, 948.0, 951.0, 954.0, 957.0, 960.0, 963.0, 966.0, 969.0, 972.0, 975.0, 978.0, 981.0, 984.0},
    {906.0, 909.0, 912.0, 915.0, 918.0, 921.0, 924.0, 927.0, 930.0, 933.0, 936.0, 939.0, 942.0, 945.0, 948.0, 951.0, 954.0, 957.0, 960.0, 963.0, 966.0, 969.0, 972.0, 975.0, 978.0, 981.0, 984.0, 987.0, 990.0},
    {912.0, 915.0, 918.0, 921.0, 924.0, 927.0, 930.0, 933.0, 936.0, 939.0, 942.0, 945.0, 948.0, 951.0, 954.0, 957.0, 960.0, 963.0, 966.0, 969.0, 972.0, 975.0, 978.0, 981.0, 984.0, 987.0, 990.0, 993.0, 996.0},
    {918.0, 921.0, 924.0, 927.0, 930.0, 933.0, 936.0, 939.0, 942.0, 945.0, 948.0, 951.0, 954.0, 957.0, 960.0, 963.0, 966.0, 969.0, 972.0, 975.0, 978.0, 981.0, 984.0, 987.0, 990.0, 993.0, 996.0, 999.0, 1002.0},
    {924.0, 927.0, 930.0, 933.0, 936.0, 939.0, 942.0, 945.0, 948.0, 951.0, 954.0, 957.0, 960.0, 963.0, 966.0, 969.0, 972.0, 975.0, 978.0, 981.0, 984.0, 987.0, 990.0, 993.0, 996.0, 999.0, 1002.0, 1005.0, 1008.0},
    {930.0, 933.0, 936.0, 939.0, 942.0, 945.0, 948.0, 951.0, 954.0, 957.0, 960.0, 963.0, 966.0, 969.0, 972.0, 975.0, 978.0, 981.0, 984.0, 987.0, 990.0, 993.0, 996.0, 999.0, 1002.0, 1005.0, 1008.0, 1011.0, 1014.0},
    {936.0, 939.0, 942.0, 945.0, 948.0, 951.0, 954.0, 957.0, 960.0, 963.0, 966.0, 969.0, 972.0, 975.0, 978.0, 981.0, 984.0, 987.0, 990.0, 993.0, 996.0, 999.0, 1002.0, 1005.0, 1008.0, 1011.0, 1014.0, 1017.0, 1020.0},
    {942.0, 945.0, 948.0, 951.0, 954.0, 957.0, 960.0, 963.0, 966.0, 969.0, 972.0, 975.0, 978.0, 981.0, 984.0, 987.0, 990.0, 993.0, 996.0, 999.0, 1002.0, 1005.0, 1008.0, 1011.0, 1014.0, 1017.0, 1020.0, 1023.0, 1026.0},
    {948.0, 951.0, 954.0, 957.0, 960.0, 963.0, 966.0, 969.0, 972.0, 975.0, 978.0, 981.0, 984.0, 987.0, 990.0, 993.0, 996.0, 999.0, 1002.0, 1005.0, 1008.0, 1011.0, 1014.0, 1017.0, 1020.0, 1023.0, 1026.0, 1029.0, 1032.0},
    {954.0, 957.0, 960.0, 963.0, 966.0, 969.0, 972.0, 975.0, 978.0, 981.0, 984.0, 987.0, 990.0, 993.0, 996.0, 999.0, 1002.0, 1005.0, 1008.0, 1011.0, 1014.0, 1017.0, 1020.0, 1023.0, 1026.0, 1029.0, 1032.0, 1035.0, 1038.0},
    {960.0, 963.0, 966.0, 969.0, 972.0, 975.0, 978.0, 981.0, 984.0, 987.0, 990.0, 993.0, 996.0, 999.0, 1002.0, 1005.0, 1008.0, 1011.0, 1014.0, 1017.0, 1020.0, 1023.0, 1026.0, 1029.0, 1032.0, 1035.0, 1038.0, 1041.0, 1044.0},
    {966.0, 969.0, 972.0, 975.0, 978.0, 981.0, 984.0, 987.0, 990.0, 993.0, 996.0, 999.0, 1002.0, 1005.0, 1008.0, 1011.0, 1014.0, 1017.0, 1020.0, 1023.0, 1026.0, 1029.0, 1032.0, 1035.0, 1038.0, 1041.0, 1044.0, 1047.0, 1050.0},
    {972.0, 975.0, 978.0, 981.0, 984.0, 987.0, 990.0, 993.0, 996.0, 999.0, 1002.0, 1005.0, 1008.0, 1011.0, 1014.0, 1017.0, 1020.0, 1023.0, 1026.0, 1029.0, 1032.0, 1035.0, 1038.0, 1041.0, 1044.0, 1047.0, 1050.0, 1053.0, 1056.0},
    {978.0, 981.0, 984.0, 987.0, 990.0, 993.0, 996.0, 999.0, 1002.0, 1005.0, 1008.0, 1011.0, 1014.0, 1017.0, 1020.0, 1023.0, 1026.0, 1029.0, 1032.0, 1035.0, 1038.0, 1041.0, 1044.0, 1047.0, 1050.0, 1053.0, 1056.0, 1059.0, 1062.0},
    {984.0, 987.0, 990.0, 993.0, 996.0, 999.0, 1002.0, 1005.0, 1008.0, 1011.0, 1014.0, 1017.0, 1020.0, 1023.0, 1026.0, 1029.0, 1032.0, 1035.0, 1038.0, 1041.0, 1044.0, 1047.0, 1050.0, 1053.0, 1056.0, 1059.0, 1062.0, 1065.0, 1068.0},
    {990.0, 993.0, 996.0, 999.0, 1002.0, 1005.0, 1008.0, 1011.0, 1014.0, 1017.0, 1020.0, 1023.0, 1026.0, 1029.0, 1032.0, 1035.0, 1038.0, 1041.0, 1044.0, 1047.0, 1050.0, 1053.0, 1056.0, 1059.0, 1062.0, 1065.0, 1068.0, 1071.0, 1074.0},
    {996.0, 999.0, 1002.0, 1005.0, 1008.0, 1011.0, 1014.0, 1017.0, 1020.0, 1023.0, 1026.0, 1029.0, 1032.0, 1035.0, 1038.0, 1041.0, 1044.0, 1047.0, 1050.0, 1053.0, 1056.0, 1059.0, 1062.0, 1065.0, 1068.0, 1071.0, 1074.0, 1077.0, 1080.0},
    {1002.0, 1005.0, 1008.0, 1011.0, 1014.0, 1017.0, 1020.0, 1023.0, 1026.0, 1029.0, 1032.0, 1035.0, 1038.0, 1041.0, 1044.0, 1047.0, 1050.0, 1053.0, 1056.0, 1059.0, 1062.0, 1065.0, 1068.0, 1071.0, 1074.0, 1077.0, 1080.0, 1083.0, 1086.0},
    {1008.0, 1011.0, 1014.0, 1017.0, 1020.0, 1023.0, 1026.0, 1029.0, 1032.0, 1035.0, 1038.0, 1041.0, 1044.0, 1047.0, 1050.0, 1053.0, 1056.0, 1059.0, 1062.0, 1065.0, 1068.0, 1071.0, 1074.0, 1077.0, 1080.0, 1083.0, 1086.0, 1089.0, 1092.0},
    {1014.0, 1017.0, 1020.0, 1023.0, 1026.0, 1029.0, 1032.0, 1035.0, 1038.0, 1041.0, 1044.0, 1047.0, 1050.0, 1053.0, 1056.0, 1059.0, 1062.0, 1065.0, 1068.0, 1071.0, 1074.0, 1077.0, 1080.0, 1083.0, 1086.0, 1089.0, 1092.0, 1095.0, 1098.0},
    {1020.0, 1023.0, 1026.0, 1029.0, 1032.0, 1035.0, 1038.0, 1041.0, 1044.0, 1047.0, 1050.0, 1053.0, 1056.0, 1059.0, 1062.0, 1065.0, 1068.0, 1071.0, 1074.0, 1077.0, 1080.0, 1083.0, 1086.0, 1089.0, 1092.0, 1095.0, 1098.0, 1101.0, 1104.0},
    {1026.0, 1029.0, 1032.0, 1035.0, 1038.0, 1041.0, 1044.0, 1047.0, 1050.0, 1053.0, 1056.0, 1059.0, 1062.0, 1065.0, 1068.0, 1071.0, 1074.0, 1077.0, 1080.0, 1083.0, 1086.0, 1089.0, 1092.0, 1095.0, 1098.0, 1101.0, 1104.0, 1107.0, 1110.0},
    {1032.0, 1035.0, 1038.0, 1041.0, 1044.0, 1047.0, 1050.0, 1053.0, 1056.0, 1059.0, 1062.0, 1065.0, 1068.0, 1071.0, 1074.0, 1077.0, 1080.0, 1083.0, 1086.0, 1089.0, 1092.0, 1095.0, 1098.0, 1101.0, 1104.0, 1107.0, 1110.0, 1113.0, 1116.0},
    {1038.0, 1041.0, 1044.0, 1047.0, 1050.0, 1053.0, 1056.0, 1059.0, 1062.0, 1065.0, 1068.0, 1071.0, 1074.0, 1077.0, 1080.0, 1083.0, 1086.0, 1089.0, 1092.0, 1095.0, 1098.0, 1101.0, 1104.0, 1107.0, 1110.0, 1113.0, 1116.0, 1119.0, 1122.0},
    {1044.0, 1047.0, 1050.0, 1053.0, 1056.0, 1059.0, 1062.0, 1065.0, 1068.0, 1071.0, 1074.0, 1077.0, 1080.0, 1083.0, 1086.0, 1089.0, 1092.0, 1095.0, 1098.0, 1101.0, 1104.0, 1107.0, 1110.0, 1113.0, 1116.0, 1119.0, 1122.0, 1125.0, 1128.0},
    {1050.0, 1053.0, 1056.0, 1059.0, 1062.0, 1065.0, 1068.0, 1071.0, 1074.0, 1077.0, 1080.0, 1083.0, 1086.0, 1089.0, 1092.0, 1095.0, 1098.0, 1101.0, 1104.0, 1107.0, 1110.0, 1113.0, 1116.0, 1119.0, 1122.0, 1125.0, 1128.0, 1131.0, 1134.0},
    {1056.0, 1059.0, 1062.0, 1065.0, 1068.0, 1071.0, 1074.0, 1077.0, 1080.0, 1083.0, 1086.0, 1089.0, 1092.0, 1095.0, 1098.0, 1101.0, 1104.0, 1107.0, 1110.0, 1113.0, 1116.0, 1119.0, 1122.0, 1125.0, 1128.0, 1131.0, 1134.0, 1137.0, 1140.0},
    {1062.0, 1065.0, 1068.0, 1071.0, 1074.0, 1077.0, 1080.0, 1083.0, 1086.0, 1089.0, 1092.0, 1095.0, 1098.0, 1101.0, 1104.0, 1107.0, 1110.0, 1113.0, 1116.0, 1119.0, 1122.0, 1125.0, 1128.0, 1131.0, 1134.0, 1137.0, 1140.0, 1143.0, 1146.0},
    {1068.0, 1071.0, 1074.0, 1077.0, 1080.0, 1083.0, 1086.0, 1089.0, 1092.0, 1095.0, 1098.0, 1101.0, 1104.0, 1107.0, 1110.0, 1113.0, 1116.0, 1119.0, 1122.0, 1125.0, 1128.0, 1131.0, 1134.0, 1137.0, 1140.0, 1143.0, 1146.0, 1149.0, 1152.0},
    {1074.0, 1077.0, 1080.0, 1083.0, 1086.0, 1089.0, 1092.0, 1095.0, 1098.0, 1101.0, 1104.0, 1107.0, 1110.0, 1113.0, 1116.0, 1119.0, 1122.0, 1125.0, 1128.0, 1131.0, 1134.0, 1137.0, 1140.0, 1143.0, 1146.0, 1149.0, 1152.0, 1155.0, 1158.0},
    {1080.0, 1083.0, 1086.0, 1089.0, 1092.0, 1095.0, 1098.0, 1101.0, 1104.0, 1107.0, 1110.0, 1113.0, 1116.0, 1119.0, 1122.0, 1125.0, 1128.0, 1131.0, 1134.0, 1137.0, 1140.0, 1143.0, 1146.0, 1149.0, 1152.0, 1155.0, 1158.0, 1161.0, 1164.0},
    {1086.0, 1089.0, 1092.0, 1095.0, 1098.0, 1101.0, 1104.0, 1107.0, 1110.0, 1113.0, 1116.0, 1119.0, 1122.0, 1125.0, 1128.0, 1131.0, 1134.0, 1137.0, 1140.0, 1143.0, 1146.0, 1149.0, 1152.0, 1155.0, 1158.0, 1161.0, 1164.0, 1167.0, 1170.0},
    {1092.0, 1095.0, 1098.0, 1101.0, 1104.0, 1107.0, 1110.0, 1113.0, 1116.0, 1119.0, 1122.0, 1125.0, 1128.0, 1131.0, 1134.0, 1137.0, 1140.0, 1143.0, 1146.0, 1149.0, 1152.0, 1155.0, 1158.0, 1161.0, 1164.0, 1167.0, 1170.0, 1173.0, 1176.0},
    {1098.0, 1101.0, 1104.0, 1107.0, 1110.0, 1113.0, 1116.0, 1119.0, 1122.0, 1125.0, 1128.0, 1131.0, 1134.0, 1137.0, 1140.0, 1143.0, 1146.0, 1149.0, 1152.0, 1155.0, 1158.0, 1161.0, 1164.0, 1167.0, 1170.0, 1173.0, 1176.0, 1179.0, 1182.0},
    {1104.0, 1107.0, 1110.0, 1113.0, 1116.0, 1119.0, 1122.0, 1125.0, 1128.0, 1131.0, 1134.0, 1137.0, 1140.0, 1143.0, 1146.0, 1149.0, 1152.0, 1155.0, 1158.0, 1161.0, 1164.0, 1167.0, 1170.0, 1173.0, 1176.0, 1179.0, 1182.0, 1185.0, 1188.0},
    {1110.0, 1113.0, 1116.0, 1119.0, 1122.0, 1125.0, 1128.0, 1131.0, 1134.0, 1137.0, 1140.0, 1143.0, 1146.0, 1149.0, 1152.0, 1155.0, 1158.0, 1161.0, 1164.0, 1167.0, 1170.0, 1173.0, 1176.0, 1179.0, 1182.0, 1185.0, 1188.0, 1191.0, 1194.0},
    {1116.0, 1119.0, 1122.0, 1125.0, 1128.0, 1131.0, 1134.0, 1137.0, 1140.0, 1143.0, 1146.0, 1149.0, 1152.0, 1155.0, 1158.0, 1161.0, 1164.0, 1167.0, 1170.0, 1173.0, 1176.0, 1179.0, 1182.0, 1185.0, 1188.0, 1191.0, 1194.0, 1197.0, 1200.0},
    {1122.0, 1125.0, 1128.0, 1131.0, 1134.0, 1137.0, 1140.0, 1143.0, 1146.0, 1149.0, 1152.0, 1155.0, 1158.0, 1161.0, 1164.0, 1167.0, 1170.0, 1173.0, 1176.0, 1179.0, 1182.0, 1185.0, 1188.0, 1191.0, 1194.0, 1197.0, 1200.0, 1203.0, 1206.0},
    {1128.0, 1131.0, 1134.0, 1137.0, 1140.0, 1143.0, 1146.0, 1149.0, 1152.0, 1155.0, 1158.0, 1161.0, 1164.0, 1167.0, 1170.0, 1173.0, 1176.0, 1179.0, 1182.0, 1185.0, 1188.0, 1191.0, 1194.0, 1197.0, 1200.0, 1203.0, 1206.0, 1209.0, 1212.0},
    {1134.0, 1137.0, 1140.0, 1143.0, 1146.0, 1149.0, 1152.0, 1155.0, 1158.0, 1161.0, 1164.0, 1167.0, 1170.0, 1173.0, 1176.0, 1179.0, 1182.0, 1185.0, 1188.0, 1191.0, 1194.0, 1197.0, 1200.0, 1203.0, 1206.0, 1209.0, 1212.0, 1215.0, 1218.0},
    {1140.0, 1143.0, 1146.0, 1149.0, 1152.0, 1155.0, 1158.0, 1161.0, 1164.0, 1167.0, 1170.0, 1173.0, 1176.0, 1179.0, 1182.0, 1185.0, 1188.0, 1191.0, 1194.0, 1197.0, 1200.0, 1203.0, 1206.0, 1209.0, 1212.0, 1215.0, 1218.0, 1221.0, 1224.0},
    {1146.0, 1149.0, 1152.0, 1155.0, 1158.0, 1161.0, 1164.0, 1167.0, 1170.0, 1173.0, 1176.0, 1179.0, 1182.0, 1185.0, 1188.0, 1191.0, 1194.0, 1197.0, 1200.0, 1203.0, 1206.0, 1209.0, 1212.0, 1215.0, 1218.0, 1221.0, 1224.0, 1227.0, 1230.0},
    {1152.0, 1155.0, 1158.0, 1161.0, 1164.0, 1167.0, 1170.0, 1173.0, 1176.0, 1179.0, 1182.0, 1185.0, 1188.0, 1191.0, 1194.0, 1197.0, 1200.0, 1203.0, 1206.0, 1209.0, 1212.0, 1215.0, 1218.0, 1221.0, 1224.0, 1227.0, 1230.0, 1233.0, 1236.0},
    {1158.0, 1161.0, 1164.0, 1167.0, 1170.0, 1173.0, 1176.0, 1179.0, 1182.0, 1185.0, 1188.0, 1191.0, 1194.0, 1197.0, 1200.0, 1203.0, 1206.0, 1209.0, 1212.0, 1215.0, 1218.0, 1221.0, 1224.0, 1227.0, 1230.0, 1233.0, 1236.0, 1239.0, 1242.0},
    {1164.0, 1167.0, 1170.0, 1173.0, 1176.0, 1179.0, 1182.0, 1185.0, 1188.0, 1191.0, 1194.0, 1197.0, 1200.0, 1203.0, 1206.0, 1209.0, 1212.0, 1215.0, 1218.0, 1221.0, 1224.0, 1227.0, 1230.0, 1233.0, 1236.0, 1239.0, 1242.0, 1245.0, 1248.0},
    {1170.0, 1173.0, 1176.0, 1179.0, 1182.0, 1185.0, 1188.0, 1191.0, 1194.0, 1197.0, 1200.0, 1203.0, 1206.0, 1209.0, 1212.0, 1215.0, 1218.0, 1221.0, 1224.0, 1227.0, 1230.0, 1233.0, 1236.0, 1239.0, 1242.0, 1245.0, 1248.0, 1251.0, 1254.0},
    {1176.0, 1179.0, 1182.0, 1185.0, 1188.0, 1191.0, 1194.0, 1197.0, 1200.0, 1203.0, 1206.0, 1209.0, 1212.0, 1215.0, 1218.0, 1221.0, 1224.0, 1227.0, 1230.0, 1233.0, 1236.0, 1239.0, 1242.0, 1245.0, 1248.0, 1251.0, 1254.0, 1257.0, 1260.0},
    {1182.0, 1185.0, 1188.0, 1191.0, 1194.0, 1197.0, 1200.0, 1203.0, 1206.0, 1209.0, 1212.0, 1215.0, 1218.0, 1221.0, 1224.0, 1227.0, 1230.0, 1233.0, 1236.0, 1239.0, 1242.0, 1245.0, 1248.0, 1251.0, 1254.0, 1257.0, 1260.0, 1263.0, 1266.0},
    {1188.0, 1191.0, 1194.0, 1197.0, 1200.0, 1203.0, 1206.0, 1209.0, 1212.0, 1215.0, 1218.0, 1221.0, 1224.0, 1227.0, 1230.0, 1233.0, 1236.0, 1239.0, 1242.0, 1245.0, 1248.0, 1251.0, 1254.0, 1257.0, 1260.0, 1263.0, 1266.0, 1269.0, 1272.0},
    {1194.0, 1197.0, 1200.0, 1203.0, 1206.0, 1209.0, 1212.0, 1215.0, 1218.0, 1221.0, 1224.0, 1227.0, 1230.0, 1233.0, 1236.0, 1239.0, 1242.0, 1245.0, 1248.0, 1251.0, 1254.0, 1257.0, 1260.0, 1263.0, 1266.0, 1269.0, 1272.0, 1275.0, 1278.0},
    {1200.0, 1203.0, 1206.0, 1209.0, 1212.0, 1215.0, 1218.0, 1221.0, 1224.0, 1227.0, 1230.0, 1233.0, 1236.0, 1239.0, 1242.0, 1245.0, 1248.0, 1251.0, 1254.0, 1257.0, 1260.0, 1263.0, 1266.0, 1269.0, 1272.0, 1275.0, 1278.0, 1281.0, 1284.0},
    {1206.0, 1209.0, 1212.0, 1215.0, 1218.0, 1221.0, 1224.0, 1227.0, 1230.0, 1233.0, 1236.0, 1239.0, 1242.0, 1245.0, 1248.0, 1251.0, 1254.0, 1257.0, 1260.0, 1263.0, 1266.0, 1269.0, 1272.0, 1275.0, 1278.0, 1281.0, 1284.0, 1287.0, 1290.0},
    {1212.0, 1215.0, 1218.0, 1221.0, 1224.0, 1227.0, 1230.0, 1233.0, 1236.0, 1239.0, 1242.0, 1245.0, 1248.0, 1251.0, 1254.0, 1257.0, 1260.0, 1263.0, 1266.0, 1269.0, 1272.0, 1275.0, 1278.0, 1281.0, 1284.0, 1287.0, 1290.0, 1293.0, 1296.0},
    {1218.0, 1221.0, 1224.0, 1227.0, 1230.0, 1233.0, 1236.0, 1239.0, 1242.0, 1245.0, 1248.0, 1251.0, 1254.0, 1257.0, 1260.0, 1263.0, 1266.0, 1269.0, 1272.0, 1275.0, 1278.0, 1281.0, 1284.0, 1287.0, 1290.0, 1293.0, 1296.0, 1299.0, 1302.0},
    {1224.0, 1227.0, 1230.0, 1233.0, 1236.0, 1239.0, 1242.0, 1245.0, 1248.0, 1251.0, 1254.0, 1257.0, 1260.0, 1263.0, 1266.0, 1269.0, 1272.0, 1275.0, 1278.0, 1281.0, 1284.0, 1287.0, 1290.0, 1293.0, 1296.0, 1299.0, 1302.0, 1305.0, 1308.0},
    {1230.0, 1233.0, 1236.0, 1239.0, 1242.0, 1245.0, 1248.0, 1251.0, 1254.0, 1257.0, 1260.0, 1263.0, 1266.0, 1269.0, 1272.0, 1275.0, 1278.0, 1281.0, 1284.0, 1287.0, 1290.0, 1293.0, 1296.0, 1299.0, 1302.0, 1305.0, 1308.0, 1311.0, 1314.0},
    {1236.0, 1239.0, 1242.0, 1245.0, 1248.0, 1251.0, 1254.0, 1257.0, 1260.0, 1263.0, 1266.0, 1269.0, 1272.0, 1275.0, 1278.0, 1281.0, 1284.0, 1287.0, 1290.0, 1293.0, 1296.0, 1299.0, 1302.0, 1305.0, 1308.0, 1311.0, 1314.0, 1317.0, 1320.0},
    {1242.0, 1245.0, 1248.0, 1251.0, 1254.0, 1257.0, 1260.0, 1263.0, 1266.0, 1269.0, 1272.0, 1275.0, 1278.0, 1281.0, 1284.0, 1287.0, 1290.0, 1293.0, 1296.0, 1299.0, 1302.0, 1305.0, 1308.0, 1311.0, 1314.0, 1317.0, 1320.0, 1323.0, 1326.0},
    {1248.0, 1251.0, 1254.0, 1257.0, 1260.0, 1263.0, 1266.0, 1269.0, 1272.0, 1275.0, 1278.0, 1281.0, 1284.0, 1287.0, 1290.0, 1293.0, 1296.0, 1299.0, 1302.0, 1305.0, 1308.0, 1311.0, 1314.0, 1317.0, 1320.0, 1323.0, 1326.0, 1329.0, 1332.0},
    {1254.0, 1257.0, 1260.0, 1263.0, 1266.0, 1269.0, 1272.0, 1275.0, 1278.0, 1281.0, 1284.0, 1287.0, 1290.0, 1293.0, 1296.0, 1299.0, 1302.0, 1305.0, 1308.0, 1311.0, 1314.0, 1317.0, 1320.0, 1323.0, 1326.0, 1329.0, 1332.0, 1335.0, 1338.0},
    {1260.0, 1263.0, 1266.0, 1269.0, 1272.0, 1275.0, 1278.0, 1281.0, 1284.0, 1287.0, 1290.0, 1293.0, 1296.0, 1299.0, 1302.0, 1305.0, 1308.0, 1311.0, 1314.0, 1317.0, 1320.0, 1323.0, 1326.0, 1329.0, 1332.0, 1335.0, 1338.0, 1341.0, 1344.0},
    {1266.0, 1269.0, 1272.0, 1275.0, 1278.0, 1281.0, 1284.0, 1287.0, 1290.0, 1293.0, 1296.0, 1299.0, 1302.0, 1305.0, 1308.0, 1311.0, 1314.0, 1317.0, 1320.0, 1323.0, 1326.0, 1329.0, 1332.0, 1335.0, 1338.0, 1341.0, 1344.0, 1347.0, 1350.0},
    {1272.0, 1275.0, 1278.0, 1281.0, 1284.0, 1287.0, 1290.0, 1293.0, 1296.0, 1299.0, 1302.0, 1305.0, 1308.0, 1311.0, 1314.0, 1317.0, 1320.0, 1323.0, 1326.0, 1329.0, 1332.0, 1335.0, 1338.0, 1341.0, 1344.0, 1347.0, 1350.0, 1353.0, 1356.0},
    {1278.0, 1281.0, 1284.0, 1287.0, 1290.0, 1293.0, 1296.0, 1299.0, 1302.0, 1305.0, 1308.0, 1311.0, 1314.0, 1317.0, 1320.0, 1323.0, 1326.0, 1329.0, 1332.0, 1335.0, 1338.0, 1341.0, 1344.0, 1347.0, 1350.0, 1353.0, 1356.0, 1359.0, 1362.0},
    {1284.0, 1287.0, 1290.0, 1293.0, 1296.0, 1299.0, 1302.0, 1305.0, 1308.0, 1311.0, 1314.0, 1317.0, 1320.0, 1323.0, 1326.0, 1329.0, 1332.0, 1335.0, 1338.0, 1341.0, 1344.0, 1347.0, 1350.0, 1353.0, 1356.0, 1359.0, 1362.0, 1365.0, 1368.0},
    {1290.0, 1293.0, 1296.0, 1299.0, 1302.0, 1305.0, 1308.0, 1311.0, 1314.0, 1317.0, 1320.0, 1323.0, 1326.0, 1329.0, 1332.0, 1335.0, 1338.0, 1341.0, 1344.0, 1347.0, 1350.0, 1353.0, 1356.0, 1359.0, 1362.0, 1365.0, 1368.0, 1371.0, 1374.0},
    {1296.0, 1299.0, 1302.0, 1305.0, 1308.0, 1311.0, 1314.0, 1317.0, 1320.0, 1323.0, 1326.0, 1329.0, 1332.0, 1335.0, 1338.0, 1341.0, 1344.0, 1347.0, 1350.0, 1353.0, 1356.0, 1359.0, 1362.0, 1365.0, 1368.0, 1371.0, 1374.0, 1377.0, 1380.0},
    {1302.0, 1305.0, 1308.0, 1311.0, 1314.0, 1317.0, 1320.0, 1323.0, 1326.0, 1329.0, 1332.0, 1335.0, 1338.0, 1341.0, 1344.0, 1347.0, 1350.0, 1353.0, 1356.0, 1359.0, 1362.0, 1365.0, 1368.0, 1371.0, 1374.0, 1377.0, 1380.0, 1383.0, 1386.0},
    {1308.0, 1311.0, 1314.0, 1317.0, 1320.0, 1323.0, 1326.0, 1329.0, 1332.0, 1335.0, 1338.0, 1341.0, 1344.0, 1347.0, 1350.0, 1353.0, 1356.0, 1359.0, 1362.0, 1365.0, 1368.0, 1371.0, 1374.0, 1377.0, 1380.0, 1383.0, 1386.0, 1389.0, 1392.0},
    {1314.0, 1317.0, 1320.0, 1323.0, 1326.0, 1329.0, 1332.0, 1335.0, 1338.0, 1341.0, 1344.0, 1347.0, 1350.0, 1353.0, 1356.0, 1359.0, 1362.0, 1365.0, 1368.0, 1371.0, 1374.0, 1377.0, 1380.0, 1383.0, 1386.0, 1389.0, 1392.0, 1395.0, 1398.0},
    {1320.0, 1323.0, 1326.0, 1329.0, 1332.0, 1335.0, 1338.0, 1341.0, 1344.0, 1347.0, 1350.0, 1353.0, 1356.0, 1359.0, 1362.0, 1365.0, 1368.0, 1371.0, 1374.0, 1377.0, 1380.0, 1383.0, 1386.0, 1389.0, 1392.0, 1395.0, 1398.0, 1401.0, 1404.0},
    {1326.0, 1329.0, 1332.0, 1335.0, 1338.0, 1341.0, 1344.0, 1347.0, 1350.0, 1353.0, 1356.0, 1359.0, 1362.0, 1365.0, 1368.0, 1371.0, 1374.0, 1377.0, 1380.0, 1383.0, 1386.0, 1389.0, 1392.0, 1395.0, 1398.0, 1401.0, 1404.0, 1407.0, 1410.0},
    {1332.0, 1335.0, 1338.0, 1341.0, 1344.0, 1347.0, 1350.0, 1353.0, 1356.0, 1359.0, 1362.0, 1365.0, 1368.0, 1371.0, 1374.0, 1377.0, 1380.0, 1383.0, 1386.0, 1389.0, 1392.0, 1395.0, 1398.0, 1401.0, 1404.0, 1407.0, 1410.0, 1413.0, 1416.0},
    {1338.0, 1341.0, 1344.0, 1347.0, 1350.0, 1353.0, 1356.0, 1359.0, 1362.0, 1365.0, 1368.0, 1371.0, 1374.0, 1377.0, 1380.0, 1383.0, 1386.0, 1389.0, 1392.0, 1395.0, 1398.0, 1401.0, 1404.0, 1407.0, 1410.0, 1413.0, 1416.0, 1419.0, 1422.0},
    {1344.0, 1347.0, 1350.0, 1353.0, 1356.0, 1359.0, 1362.0, 1365.0, 1368.0, 1371.0, 1374.0, 1377.0, 1380.0, 1383.0, 1386.0, 1389.0, 1392.0, 1395.0, 1398.0, 1401.0, 1404.0, 1407.0, 1410.0, 1413.0, 1416.0, 1419.0, 1422.0, 1425.0, 1428.0},
    {1350.0, 1353.0, 1356.0, 1359.0, 1362.0, 1365.0, 1368.0, 1371.0, 1374.0, 1377.0, 1380.0, 1383.0, 1386.0, 1389.0, 1392.0, 1395.0, 1398.0, 1401.0, 1404.0, 1407.0, 1410.0, 1413.0, 1416.0, 1419.0, 1422.0, 1425.0, 1428.0, 1431.0, 1434.0},
    {1356.0, 1359.0, 1362.0, 1365.0, 1368.0, 1371.0, 1374.0, 1377.0, 1380.0, 1383.0, 1386.0, 1389.0, 1392.0, 1395.0, 1398.0, 1401.0, 1404.0, 1407.0, 1410.0, 1413.0, 1416.0, 1419.0, 1422.0, 1425.0, 1428.0, 1431.0, 1434.0, 1437.0, 1440.0},
    {1362.0, 1365.0, 1368.0, 1371.0, 1374.0, 1377.0, 1380.0, 1383.0, 1386.0, 1389.0, 1392.0, 1395.0, 1398.0, 1401.0, 1404.0, 1407.0, 1410.0, 1413.0, 1416.0, 1419.0, 1422.0, 1425.0, 1428.0, 1431.0, 1434.0, 1437.0, 1440.0, 1443.0, 1446.0},
    {1368.0, 1371.0, 1374.0, 1377.0, 1380.0, 1383.0, 1386.0, 1389.0, 1392.0, 1395.0, 1398.0, 1401.0, 1404.0, 1407.0, 1410.0, 1413.0, 1416.0, 1419.0, 1422.0, 1425.0, 1428.0, 1431.0, 1434.0, 1437.0, 1440.0, 1443.0, 1446.0, 1449.0, 1452.0},
    {1374.0, 1377.0, 1380.0, 1383.0, 1386.0, 1389.0, 1392.0, 1395.0, 1398.0, 1401.0, 1404.0, 1407.0, 1410.0, 1413.0, 1416.0, 1419.0, 1422.0, 1425.0, 1428.0, 1431.0, 1434.0, 1437.0, 1440.0, 1443.0, 1446.0, 1449.0, 1452.0, 1455.0, 1458.0},
    {1380.0, 1383.0, 1386.0, 1389.0, 1392.0, 1395.0, 1398.0, 1401.0, 1404.0, 1407.0, 1410.0, 1413.0, 1416.0, 1419.0, 1422.0, 1425.0, 1428.0, 1431.0, 1434.0, 1437.0, 1440.0, 1443.0, 1446.0, 1449.0, 1452.0, 1455.0, 1458.0, 1461.0, 1464.0},
    {1386.0, 1389.0, 1392.0, 1395.0, 1398.0, 1401.0, 1404.0, 1407.0, 1410.0, 1413.0, 1416.0, 1419.0, 1422.0, 1425.0, 1428.0, 1431.0, 1434.0, 1437.0, 1440.0, 1443.0, 1446.0, 1449.0, 1452.0, 1455.0, 1458.0, 1461.0, 1464.0, 1467.0, 1470.0},
    {1392.0, 1395.0, 1398.0, 1401.0, 1404.0, 1407.0, 1410.0, 1413.0, 1416.0, 1419.0, 1422.0, 1425.0, 1428.0, 1431.0, 1434.0, 1437.0, 1440.0, 1443.0, 1446.0, 1449.0, 1452.0, 1455.0, 1458.0, 1461.0, 1464.0, 1467.0, 1470.0, 1473.0, 1476.0},
    {1398.0, 1401.0, 1404.0, 1407.0, 1410.0, 1413.0, 1416.0, 1419.0, 1422.0, 1425.0, 1428.0, 1431.0, 1434.0, 1437.0, 1440.0, 1443.0, 1446.0, 1449.0, 1452.0, 1455.0, 1458.0, 1461.0, 1464.0, 1467.0, 1470.0, 1473.0, 1476.0, 1479.0, 1482.0},
    {1404.0, 1407.0, 1410.0, 1413.0, 1416.0, 1419.0, 1422.0, 1425.0, 1428.0, 1431.0, 1434.0, 1437.0, 1440.0, 1443.0, 1446.0, 1449.0, 1452.0, 1455.0, 1458.0, 1461.0, 1464.0, 1467.0, 1470.0, 1473.0, 1476.0, 1479.0, 1482.0, 1485.0, 1488.0},
    {1410.0, 1413.0, 1416.0, 1419.0, 1422.0, 1425.0, 1428.0, 1431.0, 1434.0, 1437.0, 1440.0, 1443.0, 1446.0, 1449.0, 1452.0, 1455.0, 1458.0, 1461.0, 1464.0, 1467.0, 1470.0, 1473.0, 1476.0, 1479.0, 1482.0, 1485.0, 1488.0, 1491.0, 1494.0},
    {1416.0, 1419.0, 1422.0, 1425.0, 1428.0, 1431.0, 1434.0, 1437.0, 1440.0, 1443.0, 1446.0, 1449.0, 1452.0, 1455.0, 1458.0, 1461.0, 1464.0, 1467.0, 1470.0, 1473.0, 1476.0, 1479.0, 1482.0, 1485.0, 1488.0, 1491.0, 1494.0, 1497.0, 1500.0},
    {1422.0, 1425.0, 1428.0, 1431.0, 1434.0, 1437.0, 1440.0, 1443.0, 1446.0, 1449.0, 1452.0, 1455.0, 1458.0, 1461.0, 1464.0, 1467.0, 1470.0, 1473.0, 1476.0, 1479.0, 1482.0, 1485.0, 1488.0, 1491.0, 1494.0, 1497.0, 1500.0, 1503.0, 1506.0},
    {1428.0, 1431.0, 1434.0, 1437.0, 1440.0, 1443.0, 1446.0, 1449.0, 1452.0, 1455.0, 1458.0, 1461.0, 1464.0, 1467.0, 1470.0, 1473.0, 1476.0, 1479.0, 1482.0, 1485.0, 1488.0, 1491.0, 1494.0, 1497.0, 1500.0, 1503.0, 1506.0, 1509.0, 1512.0},
    {1434.0, 1437.0, 1440.0, 1443.0, 1446.0, 1449.0, 1452.0, 1455.0, 1458.0, 1461.0, 1464.0, 1467.0, 1470.0, 1473.0, 1476.0, 1479.0, 1482.0, 1485.0, 1488.0, 1491.0, 1494.0, 1497.0, 1500.0, 1503.0, 1506.0, 1509.0, 1512.0, 1515.0, 1518.0},
    {1440.0, 1443.0, 1446.0, 1449.0, 1452.0, 1455.0, 1458.0, 1461.0, 1464.0, 1467.0, 1470.0, 1473.0, 1476.0, 1479.0, 1482.0, 1485.0, 1488.0, 1491.0, 1494.0, 1497.0, 1500.0, 1503.0, 1506.0, 1509.0, 1512.0, 1515.0, 1518.0, 1521.0, 1524.0},
    {1446.0, 1449.0, 1452.0, 1455.0, 1458.0, 1461.0, 1464.0, 1467.0, 1470.0, 1473.0, 1476.0, 1479.0, 1482.0, 1485.0, 1488.0, 1491.0, 1494.0, 1497.0, 1500.0, 1503.0, 1506.0, 1509.0, 1512.0, 1515.0, 1518.0, 1521.0, 1524.0, 1527.0, 1530.0},
    {1452.0, 1455.0, 1458.0, 1461.0, 1464.0, 1467.0, 1470.0, 1473.0, 1476.0, 1479.0, 1482.0, 1485.0, 1488.0, 1491.0, 1494.0, 1497.0, 1500.0, 1503.0, 1506.0, 1509.0, 1512.0, 1515.0, 1518.0, 1521.0, 1524.0, 1527.0, 1530.0, 1533.0, 1536.0},
    {1458.0, 1461.0, 1464.0, 1467.0, 1470.0, 1473.0, 1476.0, 1479.0, 1482.0, 1485.0, 1488.0, 1491.0, 1494.0, 1497.0, 1500.0, 1503.0, 1506.0, 1509.0, 1512.0, 1515.0, 1518.0, 1521.0, 1524.0, 1527.0, 1530.0, 1533.0, 1536.0, 1539.0, 1542.0}
  }
 */
