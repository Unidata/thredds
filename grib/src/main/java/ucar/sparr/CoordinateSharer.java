package ucar.sparr;

import ucar.nc2.grib.collection.CoordinateRuntime;
import ucar.nc2.grib.collection.CoordinateTime2D;
import ucar.nc2.grib.grib2.Grib2Record;

import java.util.*;

/**
 * Create shared coordinates across variables in the same group,
 * to form the set of group coordinates.
 * Use object.equals() to find unique coordinates.
 *
 * @author John
 * @since 1/4/14
 */
public class CoordinateSharer {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CoordinateSharer.class);
  boolean runtimeUnion;

  /**
   *
   * @param runtimeUnion if true, make union of runtimes ("dense"), otherwise keep seperate runtimes if distinct
   */
  public CoordinateSharer(boolean runtimeUnion) {
    this.runtimeUnion = runtimeUnion;
  }

  List<Coordinate> unionCoords = new ArrayList<>();
  Coordinate runtimeAll;

  CoordinateBuilder runtimeAllBuilder = new CoordinateRuntime.Builder();
  Set<Coordinate> runtimeBuilders = new HashSet<>();
  Set<Coordinate> timeBuilders = new HashSet<>();
  Set<Coordinate> timeIntvBuilders = new HashSet<>();
  Set<Coordinate> vertBuilders = new HashSet<>();
  Set<Coordinate> time2DBuilders = new HashSet<>();
  Map<Coordinate, Integer> coordMap;

  public void addCoords(List<Coordinate> coords) {
    for (Coordinate coord : coords) {
      switch (coord.getType()) {
        case runtime:
          if (runtimeUnion) runtimeAllBuilder.addAll(coord); // always union
          else runtimeBuilders.add(coord);                   // unique coordinates
          break;
        case time:
          timeBuilders.add(coord);
          break;
        case timeIntv:
          timeIntvBuilders.add(coord);
          break;
        case vert:
          vertBuilders.add(coord);
          break;
        case time2D:
          CoordinateTime2D time2D = (CoordinateTime2D) coord;
          time2DBuilders.add(coord);
          //if (runtimeUnion) runtimeAllBuilder.addAll(time2D.getRuntimeCoordinate()); // ?? never ??
          //else runtimeBuilders.add(time2D.getRuntimeCoordinate());
          break;
      }
    }
  }

  public void finish() {
    if (runtimeUnion) {
      runtimeAll = runtimeAllBuilder.finish();
      unionCoords.add(runtimeAll);
    }
    for (Coordinate coord : runtimeBuilders) unionCoords.add(coord);
    for (Coordinate coord : time2DBuilders) unionCoords.add(coord);
    for (Coordinate coord : timeBuilders) unionCoords.add(coord);
    for (Coordinate coord : timeIntvBuilders) unionCoords.add(coord);
    for (Coordinate coord : vertBuilders) unionCoords.add(coord);

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
      /* if (coord.getType() == Coordinate.Type.time2D) {
        CoordinateTime2D time2D = (CoordinateTime2D) coord;
        Integer idx = coordMap.get(time2D.getRuntimeCoordinate()); // index into unionCoords
        if (idx == null) logger.error("CoordinateSharer can find runtime coordinate {}", time2D);
        else result.add(idx);
      } */

      Integer idx = coordMap.get(coord); // index into unionCoords
      if (idx == null) {
        if (coord.getType() == Coordinate.Type.runtime && runtimeUnion)  // LOOK not sure this is possible anymore
          result.add(0); // has to be 0
        else
          logger.error("CoordinateSharer can find coordinate {}", coord);

      } else {
        result.add(idx);
      }
    }
    return result;
  }

  /**
   * If using runtimeUnion, or time2D, you must reindex the sparse array
   * @param prev  previous CoordinateND
   * @return new CoordinateND containing shared coordinates and sparseArray for the new coordinates
   *   or the prev CoordinateND if reindexing not needed.
   */
  public CoordinateND<Grib2Record> reindex(CoordinateND<Grib2Record> prev) {

    boolean needReindex = false;
    for (Coordinate coord : prev.getCoordinates()) {
      if (runtimeUnion && coord.getType() == Coordinate.Type.runtime) {
        if (!coord.equals(runtimeAll))
          needReindex = true;
      }
     }

    if (!needReindex) return prev;

    List<Coordinate> completeCoords = new ArrayList<>();
    for (Coordinate coord : prev.getCoordinates()) {
      if (runtimeUnion && coord.getType() == Coordinate.Type.runtime) {
        if (!coord.equals(runtimeAll)) {
          completeCoords.add(runtimeAll);
          continue;
        }
      }

      completeCoords.add(coord);
    }

    CoordinateND<Grib2Record> result = new CoordinateND<>(completeCoords);
    result.reindex(prev);
    return result;
  }

}
