package ucar.coord;

import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;

import java.util.*;

/**
 * Create the overall coordinate across the same variable in different partitions
 * The CoordinateBuilders create unique sets of Coordinates.
 * The CoordinateND result is then the cross-product of those Coordinates.
 *
 * This is a builder helper class, the result is obtained from List<Coordinate> finish().
 *
 * So if theres a lot of missing records in that cross-product, we may have the variable wrong (?),
 *  or our assumption that the data comprises a multdim array may be wrong
 *
 * @author John
 * @since 12/10/13
 */
public class CoordinateUnionizer<T> {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CoordinateUnionizer.class);

  FeatureCollectionConfig.GribIntvFilter intvFilter;
  int varId;

  public CoordinateUnionizer(int varId, FeatureCollectionConfig.GribIntvFilter intvFilter) {
    this.intvFilter = intvFilter;
    this.varId = varId;
  }

  List<Coordinate> unionCoords = new ArrayList<>();
  // CoordinateND<GribCollection.Record> result;

  CoordinateBuilder runtimeBuilder ;
  CoordinateBuilder timeBuilder;
  CoordinateBuilder timeIntvBuilder;
  CoordinateBuilder vertBuilder;
  CoordinateBuilder ensBuilder;
  Time2DUnionBuilder time2DBuilder;

  public void addCoords(List<Coordinate> coords) {
    Coordinate runtime = null;
    for (Coordinate coord : coords) {
      switch (coord.getType()) {
        case runtime:
          CoordinateRuntime rtime = (CoordinateRuntime) coord;
          if (runtimeBuilder == null) runtimeBuilder = new CoordinateRuntime.Builder2(rtime.getTimeUnits());
          runtimeBuilder.addAll(coord);
          runtime = coord;
          break;
        case time:
          CoordinateTime time = (CoordinateTime) coord;
          if (timeBuilder == null) timeBuilder = new CoordinateTime.Builder2(coord.getCode(), time.getTimeUnit(), time.getRefDate());
          timeBuilder.addAll(coord);
          break;
        case timeIntv:
          CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) coord;
          if (timeIntvBuilder == null) timeIntvBuilder = new CoordinateTimeIntv.Builder2(null, coord.getCode(), timeIntv.getTimeUnit(), timeIntv.getRefDate());
          timeIntvBuilder.addAll(intervalFilter((CoordinateTimeIntv)coord));
          break;
        case time2D:
          CoordinateTime2D time2D = (CoordinateTime2D) coord;
          if (time2DBuilder == null) time2DBuilder = new Time2DUnionBuilder(time2D.isTimeInterval(), time2D.getTimeUnit(), coord.getCode());
          time2DBuilder.addAll(time2D);

          // debug
          CoordinateRuntime runtimeFrom2D = time2D.getRuntimeCoordinate();
          if (!runtimeFrom2D.equals(runtime))
            logger.warn("HEY CoordinateUnionizer runtimes not equal");
          break;
        case vert:
          if (vertBuilder == null) vertBuilder = new CoordinateVert.Builder2(coord.getCode());
          vertBuilder.addAll(coord);
          break;
        case ens:
          if (ensBuilder == null) ensBuilder = new CoordinateEns.Builder2(coord.getCode());
          ensBuilder.addAll(coord);
          break;
      }
    }
  }

  private List<TimeCoord.Tinv> intervalFilter(CoordinateTimeIntv coord) {
    if (intvFilter == null) return coord.getTimeIntervals();
    List<TimeCoord.Tinv> result = new ArrayList<>();
    for (TimeCoord.Tinv tinv : coord.getTimeIntervals()) {
      if (intvFilter.filterOk(varId, tinv.getIntervalSize(), 0))
        result.add(tinv);
    }
    return result;
  }

   public List<Coordinate> finish() {
    if (runtimeBuilder != null)
      unionCoords.add(runtimeBuilder.finish());
    else
      logger.warn("HEY CoordinateUnionizer missing runtime");

    if (timeBuilder != null)
      unionCoords.add(timeBuilder.finish());
    else if (timeIntvBuilder != null)
      unionCoords.add(timeIntvBuilder.finish());
    else if (time2DBuilder != null)
      unionCoords.add(time2DBuilder.finish());
    else
      logger.warn("HEY CoordinateUnionizer missing time");

    if (vertBuilder != null)
      unionCoords.add(vertBuilder.finish());
    if (ensBuilder != null)
       unionCoords.add(ensBuilder.finish());

    // result = new CoordinateND<>(unionCoords);
    return unionCoords;
  }

  /*
   * Reindex with shared coordinates and return new CoordinateND
   * @param prev  previous
   * @return new CoordinateND containing shared coordinates and sparseArray for the new coordinates
   *
  public void addIndex(CoordinateND<GribCollection.Record> prev) {
    result.reindex(prev);
  }

  public CoordinateND<GribCollection.Record> getCoordinateND() {
    return result;
  } */

  private class Time2DUnionBuilder extends CoordinateBuilderImpl<T> {
    boolean isTimeInterval;
    CalendarPeriod timeUnit;
    int code;
    SortedMap<CalendarDate, CoordinateTimeAbstract> timeMap = new TreeMap<>();

    public Time2DUnionBuilder(boolean isTimeInterval, CalendarPeriod timeUnit, int code) {
      this.isTimeInterval = isTimeInterval;
      this.timeUnit = timeUnit;
      this.code = code;
    }

    @Override
    public void addAll(Coordinate coord) {
      CoordinateTime2D coordT2D = (CoordinateTime2D) coord;
      for (int runIdx=0; runIdx<coordT2D.getNruns(); runIdx++) {  // possible duplicate runtimes from different partitions
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
      for( CalendarDate cd : timeMap.keySet()) {
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


    private CoordinateTimeAbstract testOrthogonal(Collection<CoordinateTimeAbstract> times) {
      CoordinateTimeAbstract maxCoord = null;
      int max = 0;
      Set<Object> result = new HashSet<>(100);
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

}
