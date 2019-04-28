/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.coord;

import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
import org.slf4j.LoggerFactory;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;

import java.util.*;

/**
 * Create union of CoordinateTime2D's, and/or convert CoordinateTime2D into using orthogonal or regular representation .
 * Will build orthogonal and regular if possible.
 * Does not actually depend on T.
 *
 * Used by CoordinatePartitionUnionizer to create union across partitions for one variable.
 * Used by CoordinateSharer to create common time2D across variables in same partition, when isRuntimeUnion=true.
 *
 * Used by CoordinateSharer to regularize/orthogonalize time2D coords.
 * CoordinateTime2D.Builder uses general ctor, this will build orthogonal / regular variants
 *
 * @author caron
 * @since 11/22/2014
 */
class CoordinateTime2DUnionizer<T> extends CoordinateBuilderImpl<T> {

  private boolean isTimeInterval;
  private boolean makeVals;
  private CalendarPeriod timeUnit;
  private int code;
  org.slf4j.Logger logger;
  private SortedMap<Long, CoordinateTimeAbstract> timeMap = new TreeMap<>();

  CoordinateTime2DUnionizer(boolean isTimeInterval, CalendarPeriod timeUnit, int code,
      boolean makeVals, org.slf4j.Logger logger) {
    this.isTimeInterval = isTimeInterval;
    this.timeUnit = timeUnit;
    this.code = code;
    this.makeVals = makeVals;
    this.logger = logger != null ? logger : LoggerFactory.getLogger(CoordinateTime2DUnionizer.class);
  }

  @Override
  public void addAll(Coordinate coord) {
    CoordinateTime2D coordT2D = (CoordinateTime2D) coord;
    for (int runIdx = 0; runIdx < coordT2D.getNruns(); runIdx++) {
      CoordinateTimeAbstract times = coordT2D.getTimeCoordinate(runIdx);
      long runtime = coordT2D.getRuntime(runIdx);
      timeMap.put(runtime, times);  // possible duplicate runtimes from different partitions
                                    // later partitions will override LOOK could check how many times there are and choose larger
    }
  }

  @Override
  public Object extract(T gr) {
    throw new RuntimeException();
  }

  // used when isRuntimeUnion=true
  // set the list of runtime coordinates; add any that are not already present, and make an empty CoordinateTimeAbstract for it
  void setRuntimeCoords(CoordinateRuntime runtimes) {
    for (int idx=0; idx<runtimes.getSize(); idx++) {
      CalendarDate cd = runtimes.getRuntimeDate(idx);
      long runtime = runtimes.getRuntime(idx);
      CoordinateTimeAbstract time = timeMap.get(runtime);
      if (time == null) {
        time = isTimeInterval ? new CoordinateTimeIntv(this.code, this.timeUnit, cd, new ArrayList<>(0), null) :
                new CoordinateTime(this.code, this.timeUnit, cd, new ArrayList<>(0), null);
        timeMap.put(runtime, time);
      }
    }
  }

  @Override
  public Coordinate makeCoordinate(List<Object> values) {

    // the set of unique runtimes, sorted
    List<Long> runtimes = new ArrayList<>();
    List<Coordinate> times = new ArrayList<>();  // the corresponding time coordinate for each runtime
    List<CoordinateTime2D.Time2D> allVals = new ArrayList<>();  // optionally all Time2D coordinates
    for (long runtime : timeMap.keySet()) {
      runtimes.add(runtime);
      CoordinateTimeAbstract time = timeMap.get(runtime);
      times.add(time);
      if (makeVals) {
        CalendarDate cd = CalendarDate.of(runtime);
        for (Object timeVal : time.getValues())
          allVals.add( isTimeInterval ?
                  new CoordinateTime2D.Time2D(cd, null, (TimeCoordIntvValue) timeVal) :
                  new CoordinateTime2D.Time2D(cd, (Integer) timeVal, null));
      }
    }
    Collections.sort(allVals);

    CoordinateRuntime runtime = new CoordinateRuntime(runtimes, timeUnit);

    CoordinateTimeAbstract maxCoord = testOrthogonal(timeMap.values());
    if (maxCoord != null)
      return new CoordinateTime2D(code, timeUnit, allVals, runtime, maxCoord, times, null);

    List<Coordinate> regCoords = testIsRegular();
    if (regCoords.isEmpty())
      return new CoordinateTime2D(code, timeUnit, allVals, runtime, regCoords, times, null);

    return new CoordinateTime2D(code, timeUnit, allVals, runtime, times, null);
  }

  // regular means that all the times for each offset from 0Z can be made into a single time coordinate (FMRC algo)
  private List<Coordinate> testIsRegular() {

    // group time coords by offset hour
    Map<Integer, List<CoordinateTimeAbstract>> hourMap = new TreeMap<>();
    for (CoordinateTimeAbstract coord : timeMap.values()) {
      CalendarDate runDate = coord.getRefDate();
      int hour = runDate.getHourOfDay();
      List<CoordinateTimeAbstract> hg = hourMap.computeIfAbsent(hour, k -> new ArrayList<>());
      hg.add(coord);
    }

    // see if each offset hour is orthogonal
    List<Coordinate> result = new ArrayList<>();
    for (int hour : hourMap.keySet()) {
      List<CoordinateTimeAbstract> hg = hourMap.get(hour);
      Coordinate maxCoord = testOrthogonal(hg);
      if (maxCoord == null) return ImmutableList.of();
      result.add(maxCoord);
    }
    return result;
  }

  // check if the coordinate with maximum # values includes all of the time in the collection
  // if so, we can store time2D as orthogonal
  // LOOK not right I think, consider one coordinate every 6 hours, and one every 24; should not be merged.
  @Nullable
  static CoordinateTimeAbstract testOrthogonal(Collection<CoordinateTimeAbstract> times) {
    CoordinateTimeAbstract maxCoord = null;
    Set<Object> result = new HashSet<>(100);

    int max = 0;
    for (CoordinateTimeAbstract coord : times) {
      if (max < coord.getSize()) {
        maxCoord = coord;
        max = coord.getSize();
      }

      result.addAll(coord.getValues());
    }

    // is the set of all values the same as the component times?
    // this means we can use the "orthogonal representation" of the time2D
    int totalMax = result.size();
    return totalMax == max ? maxCoord : null;
  }

}  // Time2DUnionBuilder
