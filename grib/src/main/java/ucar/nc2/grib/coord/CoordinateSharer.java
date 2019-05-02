/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.coord;

import com.google.common.collect.Sets;

import java.util.*;

/**
 * Create shared coordinate variables across the variables in the same group.
 * Use Coordinate.equals(), hashCode() to find unique coordinate variables. (so not merging similar)
 *
 * Used by GribCollectionBuilder and GribPartitionBuilder
 * This is a builder helper class.
 *
 * @author John
 * @since 1/4/14
 */
public class CoordinateSharer<T> {
  boolean isRuntimeUnion;
  org.slf4j.Logger logger;

  /**
   * @param isRuntimeUnion if true, make union of runtimes, otherwise keep separate runtimes if distinct
   */
  public CoordinateSharer(boolean isRuntimeUnion, org.slf4j.Logger logger) {
    this.isRuntimeUnion = false; // isRuntimeUnion; LOOK turn this off until we can fix it
    this.logger = logger;
  }

  Set<Coordinate> runtimeSet = new HashSet<>();
  Set<Coordinate> timeSet = new HashSet<>();
  Set<Coordinate> timeIntvSet = new HashSet<>();
  Set<Coordinate> vertSet = new HashSet<>();
  Set<Coordinate> ensSet = new HashSet<>();
  Set<Coordinate> time2DSet = new HashSet<>(); // LOOK could we use a fuzzy compare here?

  // result
  List<Coordinate> unionCoords = new ArrayList<>(); // all the coordinates in the group
  Map<Coordinate, Integer> coordMap;                // fast lookup of coordinate to list index

  // isRuntimeUnion=true only
  CoordinateRuntime.Builder2 runtimeAllBuilder;
  CoordinateTime2DUnionizer  time2DUnionizer;
  CoordinateTime2DUnionizer  timeIntv2DUnionizer;
  CoordinateRuntime runtimeAll;
  CoordinateTime2D time2Dall, timeIntv2Dall;

  // add each variable's list of coordinate
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
          else runtimeSet.add(coord);   // unique coordinates
          break;

        case time:
          timeSet.add(coord);
          break;

        case timeIntv:
          timeIntvSet.add(coord);
          break;

        case time2D:
          CoordinateTime2D time2D = (CoordinateTime2D) coord;
          if (isRuntimeUnion) {               // make union of all coordsz
            if (time2D.isTimeInterval()) {
              if (timeIntv2DUnionizer == null) timeIntv2DUnionizer = new CoordinateTime2DUnionizer(time2D.isTimeInterval(), time2D.getTimeUnit(), coord.getCode(), true, logger);
              timeIntv2DUnionizer.addAll(time2D);
            } else {
              if (time2DUnionizer == null) time2DUnionizer = new CoordinateTime2DUnionizer(time2D.isTimeInterval(), time2D.getTimeUnit(), coord.getCode(), true, logger);
              time2DUnionizer.addAll(time2D);
            }
          } else {
            time2DSet.add(coord);
          }
          // debug
          CoordinateRuntime runtimeFrom2D = time2D.getRuntimeCoordinate();
          if (!runtimeFrom2D.equals(runtime))
            logger.warn("CoordinateSharer runtimes differ");
          break;

        case vert:
          vertSet.add(coord);
          break;

        case ens:
          ensSet.add(coord);
          break;
      }
    }
  }

  private Map<Coordinate, CoordinateTime2D> swap = new HashMap<>(); // old, new coordinate
  public void finish() {
    if (isRuntimeUnion) { // have to redo any time2D with runtimeAll
      runtimeAll = (CoordinateRuntime) runtimeAllBuilder.finish();
      unionCoords.add(runtimeAll);
      if (time2DUnionizer != null) {
        time2DUnionizer.setRuntimeCoords(runtimeAll);    // make sure theres a single runtime
        time2Dall = (CoordinateTime2D) time2DUnionizer.finish();
        unionCoords.add(time2Dall);
      }
      if (timeIntv2DUnionizer != null) {
        timeIntv2DUnionizer.setRuntimeCoords(runtimeAll); // make sure theres a single runtime
        timeIntv2Dall = (CoordinateTime2D) timeIntv2DUnionizer.finish();
        unionCoords.add(timeIntv2Dall);
      }

    } else {  // not runtimeUnion

      unionCoords.addAll(runtimeSet);

      // CoordinateTime2D.Builder uses general ctor, CoordinateTime2DUnionizer will build orthogonal / regular variants
      HashSet<CoordinateTime2D> coord2Dset = new HashSet<>();
      for (Coordinate coord : time2DSet) {
        CoordinateTime2D coord2D = (CoordinateTime2D) coord;
        CoordinateTime2DUnionizer unionizer = new CoordinateTime2DUnionizer(coord2D.isTimeInterval(), coord2D.getTimeUnit(), coord2D.getCode(), true, logger);
        unionizer.addAll(coord2D);
        unionizer.finish();
        CoordinateTime2D result = (CoordinateTime2D) unionizer.getCoordinate();  // this tests for orthogonal and regular
        if (result.isOrthogonal() || result.isRegular()) {
          if (!coord2Dset.contains(result)) { // its possible that result is a duplicate CoordinateTime2D.
            unionCoords.add(result); // use the new one
            coord2Dset.add(result);
          }
          swap.put(coord2D, result); // track old, new swap

        } else {
          unionCoords.add(coord2D); // use the old one
          coord2Dset.add(coord2D);
        }
      }
    }

    unionCoords.addAll(timeSet);
    unionCoords.addAll(timeIntvSet);
    unionCoords.addAll(vertSet);
    unionCoords.addAll(ensSet);

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
    List<Integer> result = new ArrayList<>(prev.size());

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
        logger.error("CoordinateSharer cant find coordinate %s (%s)", coord.getName(), f, new Throwable());
      } else {
        result.add(idx);
      }
    }

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
    for (Coordinate coord :  this.time2DSet)
      sb.format(" %d == (%s) %s%n", coord.hashCode(), coord, coord.getName());

    sb.format("%nswap:%n");
    for (Map.Entry<Coordinate, CoordinateTime2D> entry :  this.swap.entrySet())
      sb.format(" %d (%s) %s -> %d (%s) %s%n", entry.getKey().hashCode(), entry.getKey(), entry.getKey().getName(),
              entry.getValue().hashCode(), entry.getValue(), entry.getValue().getName());
  }

  ////////////////////////////////////////////////////////////////////////////////
  // experimental

  private static final double smooshTolerence = .10;
  private List<RuntimeSmoosher> runtimes = new ArrayList<>();

  private static class RuntimeSmoosher {
    private CoordinateRuntime runtime;
    private Set<Long> coordSet = new HashSet<>();
    private boolean combined;

    RuntimeSmoosher(CoordinateRuntime runtime) {
      this.runtime = runtime;
      for (int i=0; i<runtime.getNCoords(); i++)
        coordSet.add(runtime.getRuntime(i));
    }

    // try to merge runtime whose values differ by < smooshTolerence
    public boolean nearlyEquals(RuntimeSmoosher that) {
      Sets.SetView<Long> common = Sets.intersection(this.coordSet, that.coordSet);
      int total = Math.min(this.runtime.getSize(), that.runtime.getSize());

      double nptsP = common.size() / (double) total;
      return (nptsP < smooshTolerence);
    }
  }
}
