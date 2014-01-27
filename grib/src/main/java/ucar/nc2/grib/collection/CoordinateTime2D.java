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

package ucar.nc2.grib.collection;

import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.Indent;
import ucar.sparr.Coordinate;
import ucar.sparr.CoordinateBuilderImpl;

import java.util.*;

/**
 * Both runtime and time coordinates are tracked here
 *
 * @author caron
 * @since 1/22/14
 */
public class CoordinateTime2D extends CoordinateTimeAbstract implements Coordinate {
  private final CoordinateRuntime runtime;
  private final List<Coordinate> times;

  private final List<Time2D> vals;
  private final boolean isTimeInterval;
  private final int nruns;
  private final int ntimes;

  // when reading
  public CoordinateTime2D(int code, CalendarPeriod timeUnit, CoordinateRuntime runtime, List<Coordinate> times) {
    super(code, timeUnit);

    this.runtime = runtime;
    this.times = times;
    this.isTimeInterval = times.get(0) instanceof CoordinateTimeIntv;

    int nmax = 0;
    for (Coordinate time : times)
      nmax = Math.max(nmax, time.getSize());
    ntimes = nmax;
    nruns = runtime.getSize();

    vals = new ArrayList<>();
    int runIdx = 0;
    List<CalendarDate> runs = runtime.getRuntimesSorted();
    for (Coordinate time : this.times) {
      CalendarDate run = runs.get(runIdx);
      for (Object val : time.getValues()) {
        if (isTimeInterval()) vals.add(new Time2D(run, null, (TimeCoord.Tinv) val));
        else vals.add(new Time2D(run, (Integer) val, null));
      }
      runIdx++;
    }
  }

  // when constructing
  public CoordinateTime2D(int code, CalendarPeriod timeUnit, List<Time2D> vals, CoordinateRuntime runtime, List<Coordinate> orgTimes) {
    super(code, timeUnit);

    this.runtime = runtime;
    this.isTimeInterval = orgTimes.get(0) instanceof CoordinateTimeIntv;

    int nmax = 0;
    for (Coordinate time : orgTimes)
      nmax = Math.max(nmax, time.getSize());
    ntimes = nmax;
    nruns = runtime.getSize();

    // need to make offsets from the same start date
    this.times = new ArrayList<>(orgTimes.size());
    CalendarDate firstDate = runtime.getFirstDate();
    int runIdx = 0;
    for (Coordinate orgTime : orgTimes) {
      CoordinateTimeAbstract coordTime = (CoordinateTimeAbstract) orgTime;
      CalendarPeriod period = coordTime.getPeriod();
      int offset = period.getOffset(firstDate, runtime.getDate(runIdx)); // LOOK possible loss of precision
      if (isTimeInterval)
        this.times.add( new CoordinateTimeIntv((CoordinateTimeIntv)orgTime, offset));
      else
        this.times.add( new CoordinateTime((CoordinateTime)orgTime, offset));
      runIdx++;
    }

    if (vals == null) {
      vals = new ArrayList<>();
      runIdx = 0;
      List<CalendarDate> runs = runtime.getRuntimesSorted();
      for (Coordinate time : this.times) {
        CalendarDate run = runs.get(runIdx);
        for (Object val : time.getValues()) {
          if (isTimeInterval()) vals.add(new Time2D(run, null, (TimeCoord.Tinv) val));
          else vals.add(new Time2D(run, (Integer) val, null));
        }
        runIdx++;
      }
    }
    this.vals = vals;
  }

  public CoordinateRuntime getRuntimeCoordinate() {
    return runtime;
  }

  public List<Coordinate> getTimes() {
    return times;
  }

  public boolean isTimeInterval() {
    return isTimeInterval;
  }

  public int getNtimes() {
    return ntimes;
  }

  @Override
  public void showInfo(Formatter info, Indent indent) {
    info.format("%s nruns=%d ntimes=%d total=%d", name, nruns, ntimes, vals.size());
    runtime.showInfo(info, indent);
    for (Coordinate time : times)
      time.showInfo(info, indent);
  }

  @Override
  public void showCoords(Formatter info) {
    runtime.showCoords(info);
    for (Coordinate time : times)
      time.showCoords(info);
  }

  @Override
  public List<? extends Object> getValues() {
    return vals;
  }

  @Override
  public Object getValue(int idx) {
    return vals.get(idx);
  }

  @Override
  public int getIndex(Object val) {
    return vals.indexOf(val);
  }

  @Override
  public int getSize() {
    return vals.size();
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

    if (!vals.equals(that.vals)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return vals.hashCode();
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

  /* @Override
  public CoordinateBuilder makeBuilder() {
    return new Builder(isTimeInterval, cust, timeUnit, code);
  } */

  public static class Builder extends CoordinateBuilderImpl<Grib2Record> {
    private final boolean isTimeInterval;
    private final Grib2Customizer cust;
    private final int code;                  // pdsFirst.getTimeUnit()
    private final CalendarPeriod timeUnit;

    private final CoordinateRuntime.Builder runBuilder;
    private final Map<Object, CoordinateBuilderImpl<Grib2Record>> timeBuilders;

    public Builder(boolean isTimeInterval, Grib2Customizer cust, CalendarPeriod timeUnit, int code) {
      this.isTimeInterval = isTimeInterval;
      this.cust = cust;
      this.timeUnit = timeUnit;
      this.code = code;

      runBuilder = new CoordinateRuntime.Builder();
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
        timeBuilder = isTimeInterval ? new CoordinateTimeIntv.Builder(cust, timeUnit, code) : new CoordinateTime.Builder(code, timeUnit);
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
         timeBuilder = isTimeInterval ? new CoordinateTimeIntv.Builder(cust, timeUnit, code) : new CoordinateTime.Builder(code, timeUnit);
         timeBuilders.put(val2D.run, timeBuilder);
       }
       timeBuilder.add(isTimeInterval ? val2D.tinv : val2D.time);
     }
    }

  }

}
