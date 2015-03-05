package ucar.coord;

import java.util.*;

/**
 * Create shared coordinates across variables in the same group,
 * to form the set of group coordinates.
 * Use Coordinate.equals() to find unique coordinates.
 *
 * This is a builder helper class.
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
    this.isRuntimeUnion = false; // isRuntimeUnion; LOOK turn this off until we can fix it
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
  CoordinateTime2DUnionizer  time2DUnionizer;
  CoordinateTime2DUnionizer  timeIntv2DUnionizer;
  // CoordinateTime2D.Builder2 time2DAllBuilder;
  CoordinateRuntime runtimeAll;
  CoordinateTime2D time2Dall, timeIntv2Dall;

  public void addCoordinate(Coordinate coord) {
    switch (coord.getType()) {
      case runtime:
        runtimeBuilders.add(coord);   // unique coordinates
        break;

      case time:
        timeBuilders.add(coord);
        break;

      case timeIntv:
        timeIntvBuilders.add(coord);
        break;

      case time2D:
          time2DBuilders.add(coord);
        break;

      case vert:
        vertBuilders.add(coord);
        break;

      case ens:
        ensBuilders.add(coord);
        break;
    }
  }

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
            if (time2D.isTimeInterval()) {
              if (timeIntv2DUnionizer == null) timeIntv2DUnionizer = new CoordinateTime2DUnionizer(time2D.isTimeInterval(), time2D.getTimeUnit(), coord.getCode(), true);
              timeIntv2DUnionizer.addAll(time2D);
            } else {
              if (time2DUnionizer == null) time2DUnionizer = new CoordinateTime2DUnionizer(time2D.isTimeInterval(), time2D.getTimeUnit(), coord.getCode(), true);
              time2DUnionizer.addAll(time2D);
            }
            //if (time2DAllBuilder == null)     // boolean isTimeInterval, Grib2Customizer cust, CalendarPeriod timeUnit, int code
            //  time2DAllBuilder = new CoordinateTime2D.Builder2(time2D.isTimeInterval(), null, time2D.getTimeUnit(), time2D.getCode());
            // time2DAllBuilder.addAll(coord);
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
      if (time2DUnionizer != null) {
        time2DUnionizer.setRuntimeCoords(runtimeAll);    // make sure there a single runtime
        time2Dall = (CoordinateTime2D) time2DUnionizer.finish();
        unionCoords.add(time2Dall);
      }
      if (timeIntv2DUnionizer != null) {
        timeIntv2DUnionizer.setRuntimeCoords(runtimeAll); // make sure theres a single runtime
        timeIntv2Dall = (CoordinateTime2D) timeIntv2DUnionizer.finish();
        unionCoords.add(timeIntv2Dall);
      }

    } else {  // not runtimeUnion

      for (Coordinate coord : runtimeBuilders) unionCoords.add(coord);

      // try to regularize any time2D
      HashSet<CoordinateTime2D> coord2Dset = new HashSet<>();
      for (Coordinate coord : time2DBuilders) {
        CoordinateTime2D coord2D = (CoordinateTime2D) coord;
        CoordinateTime2DUnionizer unionizer = new CoordinateTime2DUnionizer(coord2D.isTimeInterval(), coord2D.getTimeUnit(), coord2D.getCode(), true);
        unionizer.addAll(coord2D);
        unionizer.finish();
        CoordinateTime2D result = (CoordinateTime2D) unionizer.getCoordinate();  // this tests for orthogonal and regular
        if (result.isOrthogonal() || result.isRegular()) {
          if (!coord2Dset.contains(result)) { // its possible that result is a duplicate CoordinateTime2D.
            unionCoords.add(result); // use the new one
            coord2Dset.add(result);
          }
          swap.put(coord, result); // track old, new swap

        } else {
          unionCoords.add(coord2D); // use the old one
          coord2Dset.add(coord2D);
        }
      }
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

  private Map<Coordinate, Coordinate> swap = new HashMap<>();

  // this is the set of shared coordinates to be stored in the group
  public List<Coordinate> getUnionCoords() {
    return unionCoords;
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
      if (isRuntimeUnion && (coord.getType() == Coordinate.Type.runtime) && !coord.equals(runtimeAll))
          needReindex = true;
      if (null != swap.get(coord))  // time2D got swapped
        needReindex = true;
     }
    if (!needReindex) return prev;

    // need to switch out the runtime and time2D
    List<Coordinate> coords = new ArrayList<>();
    for (Coordinate prevCoord : prev.getCoordinates()) {
      if (isRuntimeUnion) {
        if (prevCoord.getType() == Coordinate.Type.runtime) {
          coords.add(runtimeAll);
        } else if (prevCoord.getType() == Coordinate.Type.time2D) {
          CoordinateTime2D time2D = (CoordinateTime2D) prevCoord;
          if (time2D.isTimeInterval())
            coords.add(timeIntv2Dall);
          else
            coords.add(time2Dall);
        } else {
          coords.add(prevCoord);
        }

      } else { // normal case - runTime2D may have gotten modified
        Coordinate newCoord = swap.get(prevCoord);
        if (newCoord != null)
          coords.add(newCoord);
        else
          coords.add(prevCoord);
      }
    }

    return new CoordinateND.Builder<T>().reindex(coords, prev);
  }

  // find indexes into unionCoords of a variable's coordinates
  public List<Integer> reindex2shared(List<Coordinate> prev) {
    List<Integer> result = new ArrayList<>();

    for (Coordinate coord : prev) {
      Coordinate swapCoord = swap.get(coord);
      if (swapCoord != null)  // time2D got swapped
        coord = swapCoord;

      Integer idx = getIndexIntoShared(coord); // index into unionCoords
      if (idx == null) {
        Formatter f = new Formatter();
        showInfo(f);
        f.format("%nprev:%n");
        for (Coordinate c :  prev)
          f.format(" %d == (%s) %s%n", c.hashCode(), c, c.getName());
        System.out.printf("%s%n", f.toString());
        logger.error("CoordinateSharer cant find coordinate "+ coord.getName(), new Throwable());
      } else
        result.add(idx);
    }

    /* debug
    for (Coordinate coord : shared) {
      switch (coord.getType()) {
        case time2D:
          CoordinateTime2D time2Dprev = (CoordinateTime2D) coord;
          Integer idx = getIndexIntoShared(coord);
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
    }  // end debug  */

    return result;
  }

  private Integer getIndexIntoShared(Coordinate prev) {   // LOOK dont understand this, why cant you just use coordMap ??
    if (isRuntimeUnion) {
      switch (prev.getType()) {
        case runtime:
          return coordMap.get(runtimeAll);
        case time2D:
          CoordinateTime2D time2D = (CoordinateTime2D) prev;
          if (time2D.isTimeInterval()) return coordMap.get(timeIntv2Dall);
          else return coordMap.get(time2Dall);
        default:
          return coordMap.get(prev);
      }
    }
    return coordMap.get(prev);
  }

  public void showInfo(Formatter sb) {
    sb.format("unionCoords:%n");
    for (Coordinate coord :  this.unionCoords)
      sb.format(" %d == (%s) %s%n", coord.hashCode(), coord, coord.getName());

    sb.format("%ncoordMap:%n");
    for (Coordinate coord :  this.coordMap.keySet())
      sb.format(" %d == (%s) %s%n", coord.hashCode(), coord, coord.getName());

    sb.format("%ntime2DBuilders:%n");
    for (Coordinate coord :  this.time2DBuilders)
      sb.format(" %d == (%s) %s%n", coord.hashCode(), coord, coord.getName());

    sb.format("%nswap:%n");
    for (Map.Entry<Coordinate, Coordinate> entry :  this.swap.entrySet())
      sb.format(" %d (%s) %s -> %d (%s) %s%n", entry.getKey().hashCode(), entry.getKey(), entry.getKey().getName(),
              entry.getValue().hashCode(), entry.getValue(), entry.getValue().getName());
  }
}
