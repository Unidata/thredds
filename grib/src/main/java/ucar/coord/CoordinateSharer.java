package ucar.coord;

import java.util.*;

/**
 * Create shared coordinates across variables in the same group,
 * to form the set of group coordinates.
 * Use object.equals() to find unique coordinates.
 *
 *  This is a builder helper class.
 *
 * @author John
 * @since 1/4/14
 */
public class CoordinateSharer<T> {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CoordinateSharer.class);

  //////////////////////////////////////////
  boolean isRuntimeUnion;

  /**
   * @param isRuntimeUnion if true, make union of runtimes, otherwise keep separate runtimes if distinct
   */
  public CoordinateSharer(boolean isRuntimeUnion) {
    this.isRuntimeUnion = isRuntimeUnion;
  }

  Set<Coordinate> runtimeBuilders = new HashSet<>();
  Set<Coordinate> timeBuilders = new HashSet<>();
  Set<Coordinate> timeIntvBuilders = new HashSet<>();
  Set<Coordinate> vertBuilders = new HashSet<>();
  Set<Coordinate> ensBuilders = new HashSet<>();
  Set<Coordinate> time2DBuilders = new HashSet<>();

  List<Coordinate> unionCoords = new ArrayList<>();
  Map<Coordinate, Integer> coordMap;

  CoordinateRuntime.Builder2 runtimeAllBuilder;
  CoordinateTime2D.Builder2 time2DAllBuilder;
  CoordinateRuntime runtimeAll;
  CoordinateTime2D time2Dall;

  // add each variable's list of coordinate
  // keep in Set, so it hold just the unique ones
  public void addCoords(List<Coordinate> coords) {
    CoordinateRuntime runtime = null;
    for (Coordinate coord : coords) {
      switch (coord.getType()) {
        case runtime:
          runtime = (CoordinateRuntime) coord;
          if (isRuntimeUnion) {               // make union of all coords
            if (runtimeAllBuilder == null)
              runtimeAllBuilder = new CoordinateRuntime.Builder2(runtime.getTimeUnits());
            runtimeAllBuilder.addAll(coord);
          }
          else runtimeBuilders.add(coord);   // unique coordinates
          break;

        case time:
          timeBuilders.add(coord);
          break;

        case timeIntv:
          timeIntvBuilders.add(coord);
          break;

        case time2D:
          CoordinateTime2D time2D = (CoordinateTime2D) coord;
          if (isRuntimeUnion) {               // make union of all coordsz
            if (time2DAllBuilder == null)     // boolean isTimeInterval, Grib2Customizer cust, CalendarPeriod timeUnit, int code
              time2DAllBuilder = new CoordinateTime2D.Builder2(time2D.isTimeInterval(), null, time2D.getTimeUnit(), time2D.getCode());
            time2DAllBuilder.addAll(coord);
          } else {
            time2DBuilders.add(coord);
          }
          // debug
          CoordinateRuntime runtimeFrom2D = time2D.getRuntimeCoordinate();
          if (!runtimeFrom2D.equals(runtime))
            System.out.println("CoordinateSharer runtimes differ");
          break;

        case vert:
          vertBuilders.add(coord);
          break;

        case ens:
          ensBuilders.add(coord);
          break;
      }
    }
  }

  //Map<CoordinateTime2D, CoordinateTime2D> resetSet;       // new, new
  //Map<Coordinate, CoordinateTime2D> convert;  // prev, new
  public void finish() {
    if (isRuntimeUnion) { // have to redo any time2D with runtimeAll
      runtimeAll = (CoordinateRuntime) runtimeAllBuilder.finish();
      unionCoords.add(runtimeAll);
      time2Dall = (CoordinateTime2D) time2DAllBuilder.finish();
      unionCoords.add(time2Dall);

      /* convert = new HashMap<>();
      resetSet = new HashMap<>();

      for (Coordinate prevCoord : time2DBuilders) {
        CoordinateTime2D reset = CoordinateTime2D.resetRuntimes((CoordinateTime2D) prevCoord, runtimeAll);
        // see if it already exists
        CoordinateTime2D already = resetSet.get(reset);
        if (already == null) {
          convert.put(prevCoord, reset);    // if not, add it
          unionCoords.add(reset);
        } else {
          convert.put(prevCoord, already);  // if so, use it
        }
      } */

    } else {
      for (Coordinate coord : runtimeBuilders) unionCoords.add(coord);
      for (Coordinate coord : time2DBuilders) unionCoords.add(coord);
    }

    for (Coordinate coord : timeBuilders) unionCoords.add(coord);
    for (Coordinate coord : timeIntvBuilders) unionCoords.add(coord);
    for (Coordinate coord : vertBuilders) unionCoords.add(coord);
    for (Coordinate coord : ensBuilders) unionCoords.add(coord);

    // fast lookup
    coordMap = new HashMap<>();
    for (int i = 0; i < this.unionCoords.size(); i++) {
      coordMap.put(this.unionCoords.get(i), i);
    }
  }

  // this is the set of shared coordinates to be stored in the group
  public List<Coordinate> getUnionCoords() {
    return unionCoords;
  }

  // find indexes into unionCoords of a variable's coordinates
  public List<Integer> reindex2shared(List<Coordinate> prev) {
    List<Integer> result = new ArrayList<>();

    for (Coordinate coord : prev) {
      Integer idx = coordMap.get(coord); // index into unionCoords
      if (idx == null) {
        if (coord.getType() == Coordinate.Type.runtime && isRuntimeUnion)
          result.add(0); // has to be 0
        else
          logger.error("CoordinateSharer cant find coordinate {}", coord);

      } else {
        result.add(idx);
      }
    }

    // debug
    for (Coordinate coord : prev) {
      switch (coord.getType()) {
        case time2D:
          CoordinateTime2D time2Dprev = (CoordinateTime2D) coord;
          Integer idx = coordMap.get(coord); // index into unionCoords
          if (idx == null)
             System.out.println("HEY");
          CoordinateTime2D time2D = (CoordinateTime2D) unionCoords.get(idx);
          int ntimePrev = time2Dprev.getNtimes();
          int ntimes = time2D.getNtimes();
          if (ntimes != ntimePrev)
            System.out.printf("HEY CoordinateSharer.reindex2shared: ntimes %d != orgNtimes %d%n", ntimes, ntimePrev);
      }
    }

    Coordinate runtime = null;
    for (Integer idx : result) {
      Coordinate coord = unionCoords.get(idx);
      switch (coord.getType()) {
        case runtime:
          runtime = coord;
          break;
        case time2D:
          CoordinateTime2D time2D = (CoordinateTime2D) coord;
          CoordinateRuntime runtimeFrom2D = time2D.getRuntimeCoordinate();
          if (!runtimeFrom2D.equals(runtime))
            System.out.printf("HEY CoordinateSharer.reindex2shared: runtimeFrom2D %s != runtime %s%n", runtimeFrom2D, runtime);

          break;
      }
    }  // end debug

    return result;
  }

  /**
   * If using runtimeUnion, or time2D, you must reindex the CoordinateND
   * @param prev  previous CoordinateND
   * @return new CoordinateND containing shared coordinates and sparseArray for the new coordinates
   *   or the prev CoordinateND if reindexing not needed.
   */
  public CoordinateND<T> reindexCoordND(CoordinateND<T> prev) {

    boolean needReindex = false;
    for (Coordinate coord : prev.getCoordinates()) {
      if (isRuntimeUnion && coord.getType() == Coordinate.Type.runtime) {
        if (!coord.equals(runtimeAll))
          needReindex = true;
      }
      if (isRuntimeUnion && coord.getType() == Coordinate.Type.time2D) {
        if (!coord.equals(time2Dall))
          needReindex = true;
      }
     }
    if (!needReindex) return prev;

    // need to switch out the runtime and time2D
    List<Coordinate> coords = new ArrayList<>();
    for (Coordinate prevCoord : prev.getCoordinates()) {
      if (prevCoord.getType() == Coordinate.Type.runtime) {
        coords.add(runtimeAll);

    } else if (prevCoord.getType() == Coordinate.Type.time2D) {
        coords.add(time2Dall);

        /* can we use the name ??
        Coordinate reset = convert.get(prevCoord);
        if (reset == null)
          logger.error("CoordinateSharer.reindexCoordND cant find reset coordinate for {}", prevCoord);
        else
          coords.add(reset); */

      } else {
        coords.add(prevCoord);
      }
    }

    return new CoordinateND.Builder<T>().reindex(coords, prev);
  }

}
