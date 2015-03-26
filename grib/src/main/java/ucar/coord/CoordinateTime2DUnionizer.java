package ucar.coord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;

import java.util.*;

/**
 * Create union  CoordinateTime2D's.
 * Will build orthogonal and regular if possible.
 * Does not actually depend on T.
 *
 * @author caron
 * @since 11/22/2014
 */
class CoordinateTime2DUnionizer<T> extends CoordinateBuilderImpl<T> {
  static private final Logger logger = LoggerFactory.getLogger(CoordinateTime2DUnionizer.class);

  boolean isTimeInterval;
  boolean makeVals;
  CalendarPeriod timeUnit;
  int code;
  SortedMap<Long, CoordinateTimeAbstract> timeMap = new TreeMap<>();
  boolean shown;

  public CoordinateTime2DUnionizer(boolean isTimeInterval, CalendarPeriod timeUnit, int code,  boolean makeVals) {
    this.isTimeInterval = isTimeInterval;
    this.timeUnit = timeUnit;
    this.code = code;
    this.makeVals = makeVals;
  }

  @Override
  public void addAll(Coordinate coord) {
    CoordinateTime2D coordT2D = (CoordinateTime2D) coord;
    for (int runIdx = 0; runIdx < coordT2D.getNruns(); runIdx++) {  // possible duplicate runtimes from different partitions
      CoordinateTimeAbstract times = coordT2D.getTimeCoordinate(runIdx);
      CoordinateTimeAbstract timesPrev = timeMap.get(coordT2D.getRuntime(runIdx));
      if (timesPrev != null && !shown) {
        logger.warn("CoordinateTime2DUnionizer duplicate runtimes from different partitions {}",
                Thread.currentThread().getStackTrace());
        shown = true;
      }
      timeMap.put(coordT2D.getRuntime(runIdx), times);   // later partitions will override LOOK could check how many times there are and choose larger
    }
  }

  @Override
  public Object extract(T gr) {
    throw new RuntimeException();
  }

  // set the list of runtime coordinates; add any that are not already present, and make an empty CoordinateTimeAbstract for it
  public void setRuntimeCoords(CoordinateRuntime runtimes) {
    for (int idx=0; idx<runtimes.getSize(); idx++) {
      CalendarDate cd = runtimes.getRuntimeDate(idx);
      long runtime = runtimes.getRuntime(idx);
      CoordinateTimeAbstract time = timeMap.get(runtime);
      if (time == null) {
        time = isTimeInterval ? new CoordinateTimeIntv(this.code, this.timeUnit, cd, new ArrayList<TimeCoord.Tinv>(0), null) :
                new CoordinateTime(this.code, this.timeUnit, cd, new ArrayList<Integer>(0), null);
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
          allVals.add( isTimeInterval ? new CoordinateTime2D.Time2D(cd, null, (TimeCoord.Tinv) timeVal) : new CoordinateTime2D.Time2D(cd, (Integer) timeVal, null));
      }
    }
    Collections.sort(allVals);

    CoordinateTimeAbstract maxCoord = testOrthogonal(timeMap.values());
    if (maxCoord != null)
      return new CoordinateTime2D(code, timeUnit, allVals, new CoordinateRuntime(runtimes, timeUnit), maxCoord, times);

    List<Coordinate> regCoords = testIsRegular();
    if (regCoords != null)
      return new CoordinateTime2D(code, timeUnit, allVals, new CoordinateRuntime(runtimes, timeUnit), regCoords, times);

    return new CoordinateTime2D(code, timeUnit, allVals, new CoordinateRuntime(runtimes, timeUnit), times);
  }

  // regular means that all the times for each offset from 0Z can be made into a single time coordinate (FMRC algo)
  private List<Coordinate> testIsRegular() {

    // group time coords by offset hour
    Map<Integer, List<CoordinateTimeAbstract>> hourMap = new TreeMap<>();
    for (CoordinateTimeAbstract coord : timeMap.values()) {
      CalendarDate runDate = coord.getRefDate();
      int hour = runDate.getHourOfDay();
      List<CoordinateTimeAbstract> hg = hourMap.get(hour);
      if (hg == null) {
        hg = new ArrayList<>();
        hourMap.put(hour, hg);
      }
      hg.add(coord);
    }

    // see if each offset hour is orthogonal
    List<Coordinate> result = new ArrayList<>();
    for (int hour : hourMap.keySet()) {
      List<CoordinateTimeAbstract> hg = hourMap.get(hour);
      Coordinate maxCoord = testOrthogonal(hg);
      if (maxCoord == null) return null;
      result.add(maxCoord);
    }
    return result;
  }

  // check if the coordinate with maximum # values includes all of the time in the collection
  // if so, we can store time2D as orthogonal
  // LOOK not right I think, consider one coordinate every 6 hours, and one every 24; should not be merged.
  static public CoordinateTimeAbstract testOrthogonal(Collection<CoordinateTimeAbstract> times) {
    CoordinateTimeAbstract maxCoord = null;
    Set<Object> result = new HashSet<>(100);

    int max = 0;
    for (CoordinateTimeAbstract coord : times) {
      if (max < coord.getSize()) {
        maxCoord = coord;
        max = coord.getSize();
      }

      for (Object val : coord.getValues())
        result.add(val);
    }

    // is the set of all values the same as the component times?
    // this means we can use the "orthogonal representation" of the time2D
    int totalMax = result.size();
    return totalMax == max ? maxCoord : null;
  }


}  // Time2DUnionBuilder
