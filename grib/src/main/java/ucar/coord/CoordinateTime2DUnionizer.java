package ucar.coord;

import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;

import java.util.*;

/**
 * Builder of CoordinateTime2D, from a set of CoordinateTime2D's.
 * Will build orthogonal and regular if possible.
 * Does not actually depend on T.
 *
 * @author caron
 * @since 11/22/2014
 */
class CoordinateTime2DUnionizer<T> extends CoordinateBuilderImpl<T> {
  boolean isTimeInterval;
  CalendarPeriod timeUnit;
  int code;
  SortedMap<CalendarDate, CoordinateTimeAbstract> timeMap = new TreeMap<>();

  public CoordinateTime2DUnionizer(boolean isTimeInterval, CalendarPeriod timeUnit, int code) {
    this.isTimeInterval = isTimeInterval;
    this.timeUnit = timeUnit;
    this.code = code;
  }

  @Override
  public void addAll(Coordinate coord) {
    CoordinateTime2D coordT2D = (CoordinateTime2D) coord;
    for (int runIdx = 0; runIdx < coordT2D.getNruns(); runIdx++) {  // possible duplicate runtimes from different partitions
      CoordinateTimeAbstract times = coordT2D.getTimeCoordinate(runIdx);
      timeMap.put(coordT2D.getRefDate(runIdx), times);          // later partitions will override LOOK could check how many times there are and choose larger
    }
  }

  @Override
  public Object extract(T gr) {
    throw new RuntimeException();
  }

  @Override
  public Coordinate makeCoordinate(List<Object> values) {

    // the set of unique runtimes
    List<CalendarDate> runtimes = new ArrayList<>();
    List<Coordinate> times = new ArrayList<>();
    for (CalendarDate cd : timeMap.keySet()) {
      runtimes.add(cd);
      times.add(timeMap.get(cd));
    }

    CoordinateTimeAbstract maxCoord = testOrthogonal(timeMap.values());
    if (maxCoord != null)
      return new CoordinateTime2D(code, timeUnit, new CoordinateRuntime(runtimes, timeUnit), maxCoord, times);

    List<Coordinate> regCoords = testIsRegular();
    if (regCoords != null)
      return new CoordinateTime2D(code, timeUnit, new CoordinateRuntime(runtimes, timeUnit), regCoords, times);

    return new CoordinateTime2D(code, timeUnit, null, new CoordinateRuntime(runtimes, timeUnit), times);
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
  CoordinateTimeAbstract testOrthogonal(Collection<CoordinateTimeAbstract> times) {
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
